package com.blazebit.persistence.impl.hibernate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.blazebit.reflection.ReflectionUtils;
import org.hibernate.HibernateException;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.dialect.Dialect;
import org.hibernate.ejb.HibernateEntityManagerImplementor;
import org.hibernate.engine.jdbc.spi.JdbcCoordinator;
import org.hibernate.engine.query.spi.HQLQueryPlan;
import org.hibernate.engine.query.spi.ParameterMetadata;
import org.hibernate.engine.spi.QueryParameters;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.TypedValue;
import org.hibernate.engine.transaction.spi.TransactionCoordinator;
import org.hibernate.event.spi.EventSource;

import com.blazebit.apt.service.ServiceProvider;
import org.hibernate.hql.internal.classic.ParserHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.loader.hql.QueryLoader;
import org.hibernate.type.Type;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

@ServiceProvider(HibernateAccess.class)
public class Hibernate4Access implements HibernateAccess {

    private static final Logger LOG = Logger.getLogger(HibernateExtendedQuerySupport.class.getName());

    @Override
    public SessionImplementor wrapSession(SessionImplementor session, boolean generatedKeys, String[][] columns, HibernateReturningResult<?> returningResult) {
        TransactionCoordinator transactionCoordinator = session.getTransactionCoordinator();
        JdbcCoordinator jdbcCoordinator = transactionCoordinator.getJdbcCoordinator();
        
        Object jdbcCoordinatorProxy = Proxy.newProxyInstance(jdbcCoordinator.getClass().getClassLoader(), new Class[]{ JdbcCoordinator.class }, new JdbcCoordinatorInvocationHandler(jdbcCoordinator, session.getFactory(), generatedKeys, columns, returningResult));
        Object transactionCoordinatorProxy = Proxy.newProxyInstance(transactionCoordinator.getClass().getClassLoader(), new Class[]{ TransactionCoordinator.class }, new Hibernate4TransactionCoordinatorInvocationHandler(transactionCoordinator, jdbcCoordinatorProxy));
        Object sessionProxy = Proxy.newProxyInstance(session.getClass().getClassLoader(), new Class[]{ SessionImplementor.class, EventSource.class }, new Hibernate4SessionInvocationHandler(session, transactionCoordinatorProxy));
        return (SessionImplementor) sessionProxy;
    }

    @Override
    public void checkTransactionSynchStatus(SessionImplementor session) {
        TransactionCoordinator coordinator = session.getTransactionCoordinator();
        coordinator.pulse();
        coordinator.getSynchronizationCallbackCoordinator().processAnyDelayedAfterCompletion();
    }

    @Override
    public void afterTransaction(SessionImplementor session, boolean success) {
        TransactionCoordinator coordinator = session.getTransactionCoordinator();
        if (!session.isTransactionInProgress() ) {
            coordinator.getJdbcCoordinator().afterTransaction();
        }
        coordinator.getSynchronizationCallbackCoordinator().processAnyDelayedAfterCompletion();
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public List<Object[]> list(QueryLoader queryLoader, SessionImplementor sessionImplementor, QueryParameters queryParameters) {
        return queryLoader.list(sessionImplementor, queryParameters);
    }

    @Override
    public List<Object> performList(HQLQueryPlan queryPlan, SessionImplementor sessionImplementor, QueryParameters queryParameters) {
        return queryPlan.performList(queryParameters, sessionImplementor);
    }

    @Override
    public int performExecuteUpdate(HQLQueryPlan queryPlan, SessionImplementor sessionImplementor, QueryParameters queryParameters) {
        return queryPlan.performExecuteUpdate(queryParameters, sessionImplementor);
    }

    @Override
    public QueryParameters getQueryParameters(Query hibernateQuery, Map<String, TypedValue> namedParams) {
        return ((org.hibernate.internal.AbstractQueryImpl) hibernateQuery).getQueryParameters(namedParams);
    }

    @Override
    public Map<String, TypedValue> getNamedParams(Query hibernateQuery) {
        return getField(hibernateQuery, "namedParameters");
    }

    @Override
    public String expandParameterLists(SessionImplementor session, org.hibernate.Query hibernateQuery, Map<String, TypedValue> namedParamsCopy) {
        String query = hibernateQuery.getQueryString();
        ParameterMetadata parameterMetadata = getParameterMetadata(hibernateQuery);
        Iterator<Map.Entry<String, TypedValue>> iter = getNamedParamLists(hibernateQuery).entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, TypedValue> me = iter.next();
            query = expandParameterList(session, parameterMetadata, query, (String) me.getKey(), (TypedValue) me.getValue(), namedParamsCopy);
        }
        return query;
    }

    private ParameterMetadata getParameterMetadata(org.hibernate.Query hibernateQuery) {
        return getField(hibernateQuery, "parameterMetadata");
    }

    private Map<String, TypedValue> getNamedParamLists(org.hibernate.Query hibernateQuery) {
        return getField(hibernateQuery, "namedParameterLists");
    }

    private String expandParameterList(SessionImplementor session, ParameterMetadata parameterMetadata, String query, String name, TypedValue typedList, Map<String, TypedValue> namedParamsCopy) {
        Collection<?> vals = (Collection<?>) typedList.getValue();

        // HHH-1123
        // Some DBs limit number of IN expressions.  For now, warn...
        final Dialect dialect = session.getFactory().getDialect();
        final int inExprLimit = dialect.getInExpressionCountLimit();
        if (inExprLimit > 0 && vals.size() > inExprLimit) {
            LOG.warning(String.format("Dialect [%s] limits the number of elements in an IN predicate to %s entries.  " +
                    "However, the given parameter list [%s] contained %s entries, which will likely cause failures " +
                    "to execute the query in the database", dialect.getClass().getName(), inExprLimit, name, vals.size()));
        }

        Type type = typedList.getType();

        boolean isJpaPositionalParam = parameterMetadata.getNamedParameterDescriptor(name).isJpaStyle();
        String paramPrefix = isJpaPositionalParam ? "?" : ParserHelper.HQL_VARIABLE_PREFIX;
        String placeholder =
                new StringBuilder(paramPrefix.length() + name.length())
                        .append(paramPrefix).append(name)
                        .toString();

        if (query == null) {
            return query;
        }
        int loc = query.indexOf(placeholder);

        if (loc < 0) {
            return query;
        }

        String beforePlaceholder = query.substring(0, loc);
        String afterPlaceholder =  query.substring(loc + placeholder.length());

        // check if placeholder is already immediately enclosed in parentheses
        // (ignoring whitespace)
        boolean isEnclosedInParens =
                StringHelper.getLastNonWhitespaceCharacter(beforePlaceholder) == '(' &&
                        StringHelper.getFirstNonWhitespaceCharacter(afterPlaceholder) == ')';

        if (vals.size() == 1  && isEnclosedInParens) {
            // short-circuit for performance when only 1 value and the
            // placeholder is already enclosed in parentheses...
            namedParamsCopy.put(name, new TypedValue(type, vals.iterator().next()));
            return query;
        }

        StringBuilder list = new StringBuilder(16);
        Iterator<?> iter = vals.iterator();
        int i = 0;
        while (iter.hasNext()) {
            // Variable 'name' can represent a number or contain digit at the end. Surrounding it with
            // characters to avoid ambiguous definition after concatenating value of 'i' counter.
            String alias = (isJpaPositionalParam ? 'x' + name : name) + '_' + i++ + '_';
            if (namedParamsCopy.put(alias, new TypedValue(type, iter.next())) != null) {
                throw new HibernateException("Repeated usage of alias '" + alias + "' while expanding list parameter.");
            }
            list.append(ParserHelper.HQL_VARIABLE_PREFIX).append(alias);
            if (iter.hasNext()) {
                list.append(", ");
            }
        }
        return StringHelper.replace(
                beforePlaceholder,
                afterPlaceholder,
                placeholder.toString(),
                list.toString(),
                true,
                true
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T getField(Object object, String field) {
        boolean madeAccessible = false;
        Field f = null;
        try {
            f = ReflectionUtils.getField(object.getClass(), field);
            madeAccessible = !f.isAccessible();

            if (madeAccessible) {
                f.setAccessible(true);
            }
            return (T) f.get(object);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        } finally {
            if (madeAccessible) {
                f.setAccessible(false);
            }
        }
    }

    private HibernateEntityManagerImplementor getEntityManager(EntityManager em) {
        return (HibernateEntityManagerImplementor) em.unwrap(EntityManager.class);
    }

    @Override
    public RuntimeException convert(EntityManager em, HibernateException e) {
        return getEntityManager(em).convert(e);
    }

    @Override
    public void handlePersistenceException(EntityManager em, PersistenceException e) {
        getEntityManager(em).handlePersistenceException(e);
    }

    @Override
    public void throwPersistenceException(EntityManager em, HibernateException e) {
        getEntityManager(em).throwPersistenceException(e);
    }

    @Override
    public QueryParameters createQueryParameters(
            final Type[] positionalParameterTypes,
            final Object[] positionalParameterValues,
            final Map<String,TypedValue> namedParameters,
            final LockOptions lockOptions,
            final RowSelection rowSelection,
            final boolean isReadOnlyInitialized,
            final boolean readOnly,
            final boolean cacheable,
            final String cacheRegion,
            //final boolean forceCacheRefresh,
            final String comment,
            final List<String> queryHints,
            final Serializable[] collectionKeys) {
        return new QueryParameters(
                positionalParameterTypes,
                positionalParameterValues,
                namedParameters,
                lockOptions,
                rowSelection,
                isReadOnlyInitialized,
                readOnly,
                cacheable,
                cacheRegion,
                comment,
                collectionKeys,
                null
        );
    }
}