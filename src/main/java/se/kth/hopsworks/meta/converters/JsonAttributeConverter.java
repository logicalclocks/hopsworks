/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.meta.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import javax.persistence.AttributeConverter;
import org.codehaus.jackson.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * *
 * Usage:
 *
 * Add this line to fields in Entity beans that should be persisted as MySQL JSON type
 *
 * @Convert(converter = JpaConverterJson.class)
 *
 * TODO- bug when persisting!
 *
 * @author jdowling
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
