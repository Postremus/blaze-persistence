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

package com.blazebit.persistence.spi;

import java.util.Map;
import java.util.Set;

/**
 * A structure for accessing join table information.
 *
 * @author Christian Beikov
 * @since 1.2.0
 */
public class JoinTable {

    private final String tableName;
    private final Set<String> idAttributeNames;
    private final Set<String> targetAttributeNames;
    private final Map<String, String> idColumnMappings;
    private final Map<String, String> keyColumnMappings;
    private final Map<String, String> keyColumnTypes;
    private final Map<String, String> targetIdColumnMappings;

    /**
     * Constructs a JoinTable.
     * @param tableName The join table name
     * @param idAttributeNames The attribute names of the owning entity
     * @param idColumnMappings The id column mappings
     * @param keyColumnMappings The key column mappings
     * @param keyColumnTypes The key column types
     * @param targetAttributeNames The attribute names of the target entity
     * @param targetIdColumnMappings The target id column mappings
     */
    public JoinTable(String tableName, Set<String> idAttributeNames, Map<String, String> idColumnMappings, Map<String, String> keyColumnMappings, Map<String, String> keyColumnTypes, Set<String> targetAttributeNames, Map<String, String> targetIdColumnMappings) {
        this.tableName = tableName;
        this.idAttributeNames = idAttributeNames;
        this.idColumnMappings = idColumnMappings;
        this.keyColumnMappings = keyColumnMappings;
        this.keyColumnTypes = keyColumnTypes;
        this.targetAttributeNames = targetAttributeNames;
        this.targetIdColumnMappings = targetIdColumnMappings;
    }

    /**
     * The name of the join table.
     *
     * @return The join table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Returns the id attribute names of the owning entity that map to the join table.
     *
     * @return The owner entity id attribute names
     * @since 1.3.0
     */
    public Set<String> getIdAttributeNames() {
        return idAttributeNames;
    }

    /**
     * Returns the foreign key mappings of the join tables column names to the owner table column names.
     * The keys are column names of the join table. Values are the column names of the owner table.
     *
     * @return The id column mappings
     */
    public Map<String, String> getIdColumnMappings() {
        return idColumnMappings;
    }

    /**
     * Returns the column mappings of the key/index column if there are any, otherwise <code>null</code>.
     * In case of a simple key/index type, this returns the column name mapping to itself.
     * Otherwise returns foreign key mappings of the join tables key column names to the key target table column names.
     * The keys are column names of the join table. Values are the column names of the key target table.
     *
     * @return The key column mappings
     */
    public Map<String, String> getKeyColumnMappings() {
        return keyColumnMappings;
    }

    /**
     * Returns the column types of the key/index columns if there are any, otherwise <code>null</code>.
     *
     * @return The key column types
     * @since 1.3.0
     */
    public Map<String, String> getKeyColumnTypes() {
        return keyColumnTypes;
    }

    /**
     * Returns the id attribute names of the target entity that map to the join table.
     *
     * @return The target entity id attribute names
     * @since 1.3.0
     */
    public Set<String> getTargetAttributeNames() {
        return targetAttributeNames;
    }

    /**
     * Returns the foreign key mappings of the join tables column names to the target table column names.
     * The keys are column names of the join table. Values are the column names of the target table.
     *
     * @return The target id column mappings
     */
    public Map<String, String> getTargetColumnMappings() {
        return targetIdColumnMappings;
    }
}
