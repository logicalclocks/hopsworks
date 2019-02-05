/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.api.metadata.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.persistence.AttributeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * *
 * Usage:
 * <p>
 * Add this line to fields in Entity beans that should be persisted as MySQL
 * JSON type
 *
 * @Convert(converter = JpaConverterJson.class)
 *
 * TODO- bug when persisting!
 *
 * @param <T>
 */
public class JsonAttributeConverter<T extends Object> implements
        AttributeConverter<List<JsonDocuments>, String> {

  private final Class<T> clazz;

  public JsonAttributeConverter(Class<T> clazz) {
    this.clazz = clazz;
  }

  // ObjectMapper is thread safe
  private final static ObjectMapper objectMapper = new ObjectMapper();

  private Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public String convertToDatabaseColumn(List<JsonDocuments> meta) {
    String jsonString = "";
    try {
      log.debug("Start convertToDatabaseColumn");
      // convert list of POJO to json
      jsonString = objectMapper.writeValueAsString(meta);
      log.debug("convertToDatabaseColumn" + jsonString);
    } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
      log.error(ex.getMessage());
    }
    return jsonString;
  }

  @Override
  public List<JsonDocuments> convertToEntityAttribute(String dbData) {
    List<JsonDocuments> list = new ArrayList<JsonDocuments>();
    try {
      log.debug("Start convertToEntityAttribute");

      // convert json to list of POJO
      list = Arrays.asList(objectMapper.readValue(dbData,
              JsonDocuments[].class));
      log.debug("JsonDocumentsConverter.convertToDatabaseColumn" + list);

    } catch (IOException ex) {
      log.error(ex.getMessage());
    }
    return list;
  }

//  @Override
//  public byte[] convertToDatabaseColumn(Object attribute) {
//    try {
//      return objectMapper.writeValueAsString(attribute).getBytes();
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
//  }
//
//  @Override
//  public T convertToEntityAttribute(byte[] dbData) {
//    try {
//      return objectMapper.readValue(dbData, clazz);
//    } catch (Exception e) {
//      throw new RuntimeException(e);
//    }
//  }
}
