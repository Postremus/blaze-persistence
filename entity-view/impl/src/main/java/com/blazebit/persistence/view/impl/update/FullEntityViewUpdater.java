package com.blazebit.persistence.view.impl.update;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import com.blazebit.persistence.view.impl.proxy.UpdateableProxy;
import com.blazebit.persistence.view.impl.tx.TransactionHelper;
import com.blazebit.persistence.view.impl.tx.TransactionSynchronizationStrategy;
import com.blazebit.persistence.view.metamodel.MappingAttribute;
import com.blazebit.persistence.view.metamodel.MethodAttribute;
import com.blazebit.persistence.view.metamodel.ViewType;

public class FullEntityViewUpdater implements EntityViewUpdater {

	private final String idAttributeName;
	private final String[] dirtyStateAttributeNames;
	private final int[] dirtyStateIndexToInitialStateIndexMapping;
	private final String updateQuery;

    @SuppressWarnings({ "unchecked", "rawtypes" })
	public FullEntityViewUpdater(ViewType<?> viewType) {
        Class<?> entityClass = viewType.getEntityClass();
		Set<MethodAttribute<?, ?>> attributes = (Set<MethodAttribute<?, ?>>) (Set) viewType.getAttributes();
        MethodAttribute<?, ?> idAttribute = viewType.getIdAttribute();
        List<String> attributeList = new ArrayList<String>(attributes.size());
        List<Integer> indexMapping = new ArrayList<Integer>(attributes.size());
        StringBuilder sb = new StringBuilder(100);

        sb.append("UPDATE " + entityClass.getName() + " SET ");
        
        int i = 1;
        boolean first = true;
        for (MethodAttribute<?, ?> attribute : attributes) {
        	if (attribute == idAttribute) {
        		continue;
        	}
        	
        	if (attribute.isUpdateable()) {
	            if (first) {
	                first = false;
	            } else {
	                sb.append(", ");
	            }
	            
	            sb.append(((MappingAttribute<?, ?>) viewType.getAttribute(attribute.getName())).getMapping());
	            sb.append(" = :");
        		sb.append(attribute.getName());
        		attributeList.add(attribute.getName());
        		indexMapping.add(i);
        	}
        	
        	i++;
        }
        
        dirtyStateAttributeNames = attributeList.toArray(new String[attributeList.size()]);
        dirtyStateIndexToInitialStateIndexMapping = new int[indexMapping.size()];
        
        for (i = 0; i < dirtyStateIndexToInitialStateIndexMapping.length; i++) {
        	dirtyStateIndexToInitialStateIndexMapping[i] = indexMapping.get(i);
        }

        String idName = idAttribute.getName();
        sb.append(" WHERE ").append(((MappingAttribute<?, ?>) viewType.getAttribute(idName)).getMapping()).append(" = :").append(idName);
        idAttributeName = idName;
        updateQuery = sb.toString();
	}

    @Override
	public void executeUpdate(EntityManager em, UpdateableProxy updateableProxy) {
        TransactionSynchronizationStrategy synchronizationStrategy = TransactionHelper.getSynchronizationStrategy(em);
        
        if (!synchronizationStrategy.isActive()) {
			throw new IllegalStateException("Transaction is not active!");
        }
		
        Object id = updateableProxy.$$_getId();
        Object[] initialState = updateableProxy.$$_getInitialState();
        Object[] originalDirtyState = updateableProxy.$$_getDirtyState();

        // Copy the dirtyState because until transaction commit, there might still happen some changes
        Object[] dirtyState = originalDirtyState.clone();
        Query query = em.createQuery(updateQuery);
        query.setParameter(idAttributeName, id);

        for (int i = 0; i < dirtyState.length; i++) {
        	if (dirtyState[i] != null) {
        		query.setParameter(dirtyStateAttributeNames[i], dirtyState[i]);
        	} else {
        		query.setParameter(dirtyStateAttributeNames[i], initialState[dirtyStateIndexToInitialStateIndexMapping[i]]);
        	}
        }
        
        int updatedCount = query.executeUpdate();
        
        if (updatedCount != 1) {
            throw new RuntimeException("Update did not work! Expected to update 1 row but was: " + updatedCount);
        }
        
        synchronizationStrategy.registerSynchronization(new ClearDirtySynchronization(initialState, originalDirtyState, dirtyState, dirtyStateIndexToInitialStateIndexMapping));
	}
}