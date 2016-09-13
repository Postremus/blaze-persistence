/*
 * Copyright 2014 Blazebit.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.blazebit.persistence.view.impl.metamodel.attribute;

import com.blazebit.persistence.impl.expression.ExpressionFactory;
import com.blazebit.persistence.view.impl.metamodel.AbstractMethodMappingPluralAttribute;
import com.blazebit.persistence.view.impl.metamodel.EntityMetamodel;
import com.blazebit.persistence.view.impl.metamodel.MetamodelUtils;
import com.blazebit.persistence.view.metamodel.ListAttribute;
import com.blazebit.persistence.view.metamodel.ManagedViewType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public abstract class AbstractMethodMappingListAttribute<X, Y> extends AbstractMethodMappingPluralAttribute<X, List<Y>, Y> implements ListAttribute<X, Y> {

    private final boolean isIndexed;
    
    public AbstractMethodMappingListAttribute(ManagedViewType<X> viewType, Method method, Annotation mapping, Set<Class<?>> entityViews, EntityMetamodel metamodel, ExpressionFactory expressionFactory) {
        super(viewType, method, mapping, entityViews, false);
        
        if (isIgnoreIndex()) {
        	this.isIndexed = false;
        } else {
        	this.isIndexed = MetamodelUtils.isIndexedList(metamodel, expressionFactory, viewType.getEntityClass(), mapping);
        }
    }

    @Override
    public CollectionType getCollectionType() {
        return CollectionType.LIST;
    }

    @Override
    public boolean isSorted() {
        return false;
    }

    @Override
    public boolean isIndexed() {
        return isIndexed;
    }

    @Override
    public boolean isOrdered() {
        return !isIndexed;
    }

}