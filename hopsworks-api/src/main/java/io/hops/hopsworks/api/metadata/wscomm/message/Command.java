/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.metadata.wscomm.message;

/**
 * Represents the command each incoming message is transformed to
 */
public enum Command {

  STORE_FIELD,
  //
  STORE_TEMPLATE,
  //
  EXTEND_TEMPLATE,
  //
  STORE_METADATA,
  //
  ADD_NEW_TEMPLATE,
  //
  REMOVE_TEMPLATE,
  //
  FETCH_TEMPLATE,
  //
  FETCH_TEMPLATES,
  //
  FETCH_METADATA,
  //
  FETCH_TABLE_METADATA,
  //
  FETCH_FIELD_TYPES,
  //
  DELETE_TABLE,
  //
  DELETE_FIELD,
  //
  BROADCAST,
  //
  QUIT,
  //
  TEST,
  //
  IS_TABLE_EMPTY,
  //
  IS_FIELD_EMPTY,
  //
  UPDATE_METADATA,
  //
  REMOVE_METADATA,
  //
  UPDATE_TEMPLATE_NAME,
  //
  RENAME_DIR,
  //
  CREATE_META_LOG
}
