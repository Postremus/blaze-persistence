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

package com.blazebit.persistence.view.testsuite.basic.model;

import java.util.Map;
import java.util.Set;

import com.blazebit.persistence.view.EntityView;
import com.blazebit.persistence.view.Mapping;
import com.blazebit.persistence.view.testsuite.entity.EmbeddableTestEntity2;
import com.blazebit.persistence.view.testsuite.entity.EmbeddableTestEntityId2;

/**
 *
 * @author Christian Beikov
 * @since 1.0.0
 */
@EntityView(EmbeddableTestEntity2.class)
public interface EmbeddableTestEntityViewWithSubview extends IdHolderView<EmbeddableTestEntityId2> {

    @Mapping("id.intIdEntity")
    public IntIdEntityView getIdIntIdEntity();
    
    @Mapping("id.intIdEntity.id")
    public Integer getIdIntIdEntityId();
    
    @Mapping("id.intIdEntity.name")
    public String getIdIntIdEntityName();
    
    @Mapping("id.key")
    public String getIdKey();

    @Mapping("embeddable")
    public EmbeddableTestEntityEmbeddableSubView getEmbeddable();

    @Mapping("embeddableSet")
    public Set<EmbeddableTestEntitySimpleEmbeddableSubView> getEmbeddableSet();

    @Mapping("embeddableMap")
    public Map<String, EmbeddableTestEntitySimpleEmbeddableSubView> getEmbeddableMap();

    @Mapping("embeddable.manyToOne")
    public EmbeddableTestEntitySubView getEmbeddableManyToOneView();

    @Mapping("embeddable.oneToMany")
    public Set<EmbeddableTestEntitySubView> getEmbeddableOneToManyView();

    @Mapping("embeddable.elementCollection")
    public Map<String, IntIdEntityView> getEmbeddableElementCollectionView();
}
