/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
