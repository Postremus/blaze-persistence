/*
 * Copyright 2014 - 2020 Blazebit.
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

package com.blazebit.persistence.integration.quarkus.runtime;

import com.blazebit.persistence.Criteria;
import com.blazebit.persistence.spi.CriteriaBuilderConfiguration;
import com.blazebit.persistence.view.ConfigurationProperties;
import com.blazebit.persistence.view.EntityViews;
import com.blazebit.persistence.view.spi.EntityViewConfiguration;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.annotations.Recorder;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Moritz Becker
 * @since 1.5.0
 */
@Recorder
public class EntityViewRecorder {

    private List<Class<?>> entityViews = new ArrayList<>();
    private List<Class<?>> entityViewListeners = new ArrayList<>();

    public void addEntityView(Class<?> entityView) {
        this.entityViews.add(entityView);
    }

    public void addEntityViewListener(Class<?> entityViewListener) {
        this.entityViewListeners.add(entityViewListener);
    }

    public BeanContainerListener setCriteriaBuilderConfiguration(BlazePersistenceConfiguration blazePersistenceConfig) {
        return beanContainer -> {
            CriteriaBuilderConfiguration criteriaBuilderConfiguration = Criteria.getDefault();
            blazePersistenceConfig.apply(criteriaBuilderConfiguration);
            CriteriaBuilderConfigurationHolder configurationHolder = beanContainer.instance(CriteriaBuilderConfigurationHolder.class);
            configurationHolder.setCriteriaBuilderConfiguration(criteriaBuilderConfiguration);
        };
    }

    public BeanContainerListener setEntityViewConfiguration(BlazePersistenceConfiguration blazePersistenceConfig) {
        return beanContainer -> {
            EntityViewConfigurationHolder configurationHolder = beanContainer.instance(EntityViewConfigurationHolder.class);
            EntityViewConfiguration entityViewConfiguration = EntityViews.createDefaultConfiguration();
            for (Class<?> entityView : entityViews) {
                entityViewConfiguration.addEntityView(entityView);
            }
            for (Class<?> entityViewListener : entityViewListeners) {
                entityViewConfiguration.addEntityViewListener(entityViewListener);
            }
            blazePersistenceConfig.apply(entityViewConfiguration);
            entityViewConfiguration.setProperty(ConfigurationProperties.PROXY_UNSAFE_ALLOWED, Boolean.FALSE.toString());
            configurationHolder.setEntityViewConfiguration(entityViewConfiguration);
        };
    }
}