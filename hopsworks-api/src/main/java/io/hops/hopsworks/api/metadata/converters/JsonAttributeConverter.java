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
