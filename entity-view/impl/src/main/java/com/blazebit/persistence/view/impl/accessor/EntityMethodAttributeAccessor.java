/*
 * Copyright 2014 - 2022 Blazebit.
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

package com.blazebit.persistence.view.impl.accessor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author Christian Beikov
 * @since 1.2.0
 */
public class EntityMethodAttributeAccessor implements AttributeAccessor {

    private final Method getter;
    private final Method setter;
    private final Constructor<?> targetTypeConstructor;

    EntityMethodAttributeAccessor(Method getter, Method setter) {
        this(getter, setter, null);
    }

    EntityMethodAttributeAccessor(Method getter, Method setter, Class<?> targetType) {
        getter.setAccessible(true);
        setter.setAccessible(true);
        this.getter = getter;
        this.setter = setter;
        if (targetType == null) {
            this.targetTypeConstructor = null;
        } else {
            try {
                Constructor<?> declaredConstructor = targetType.getDeclaredConstructor();
                declaredConstructor.setAccessible(true);
                this.targetTypeConstructor = declaredConstructor;
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException("The following type is missing a default constructor: " + targetType.getName());
            }
        }
    }

    @Override
    public void setValue(Object entity, Object value) {
        try {
            setter.invoke(entity, value);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't map value [" + value + "] to entity attribute!", e);
        }
    }

    @Override
    public Object getOrCreateValue(Object entity) {
        if (entity == null) {
            return null;
        }

        try {
            Object result = getter.invoke(entity);
            if (result == null && targetTypeConstructor != null) {
                result = targetTypeConstructor.newInstance();
                setValue(entity, result);
            }

            return result;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't access entity attribute value!", e);
        }
    }

    @Override
    public Object getValue(Object entity) {
        if (entity == null) {
            return null;
        }

        try {
            return getter.invoke(entity);
        } catch (Exception e) {
            throw new RuntimeException("Couldn't access entity attribute value!", e);
        }
    }
}
