/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.vectordb;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface VectorDatabase {
  Optional<Index> getIndex(String name) throws VectorDatabaseException;
  Set<Index> getAllIndices() throws VectorDatabaseException;
  void createIndex(Index index, String mapping, Boolean skipIfExist) throws VectorDatabaseException;
  void deleteIndex(Index index) throws VectorDatabaseException;
  void addFields(Index index, String mapping) throws VectorDatabaseException;
  List<Field> getSchema(Index index) throws VectorDatabaseException;
  void writeMap(Index index, Map<String, Object> data) throws VectorDatabaseException;
  void writeMap(Index index, Map<String, Object> data, String docId) throws VectorDatabaseException;
  void batchWriteMap(Index index, List<Map<String, Object>> data) throws VectorDatabaseException;
  void batchWriteMap(Index index, Map<String, Map<String, Object>> data) throws VectorDatabaseException;
  void write(Index index, String data) throws VectorDatabaseException;
  void write(Index index, String data, String docId) throws VectorDatabaseException;
  void batchWrite(Index index, List<String> data) throws VectorDatabaseException;
  void batchWrite(Index index, Map<String, String> data) throws VectorDatabaseException;
  void deleteByQuery(Index index, String query) throws VectorDatabaseException;
  List<Map<String, Object>> preview(Index index, Set<Field> fields, int n) throws VectorDatabaseException;
  void close();
}
