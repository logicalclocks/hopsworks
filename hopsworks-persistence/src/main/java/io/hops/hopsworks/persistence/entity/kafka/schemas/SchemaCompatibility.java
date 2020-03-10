/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */
package io.hops.hopsworks.persistence.entity.kafka.schemas;

/* The values of SchemaCompatibility are defined in the Confluent Schema Registry v5.3.1 docs.
 * Link: https://docs.confluent.io/5.3.1/schema-registry/develop/api.html#compatibility
 */
public enum SchemaCompatibility {
  BACKWARD,
  BACKWARD_TRANSITIVE,
  FORWARD,
  FORWARD_TRANSITIVE,
  FULL,
  FULL_TRANSITIVE,
  NONE
}
