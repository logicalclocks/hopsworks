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

package io.hops.hopsworks.api.models;

import io.hops.hopsworks.api.models.dto.ModelDTO;
import io.hops.hopsworks.exceptions.ModelsException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.json.JSONObject;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ModelConverter {
  
  private JAXBContext jaxbModelsContext;
  
  @PostConstruct
  public void init() {
    try {
      Class[] serializedClasses = new Class[] {
        ModelDTO.class
      };
      jaxbModelsContext = JAXBContextFactory.
        createContext(serializedClasses, null);
    } catch (JAXBException e) {
      e.printStackTrace();
    }
  }
  
  private Marshaller createMarshaller() throws ModelsException {
    try {
      Marshaller marshaller = jaxbModelsContext.createMarshaller();
      marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      return marshaller;
    } catch(JAXBException e) {
      throw new ModelsException(RESTCodes.ModelsErrorCode.MODEL_MARSHALLING_FAILED, Level.INFO,
        "Failed to marshal", "Error occurred during marshalling setup", e);
    }
  }
  
  private Unmarshaller createUnmarshaller() throws ModelsException {
    try {
      Unmarshaller unmarshaller = jaxbModelsContext.createUnmarshaller();
      unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      return unmarshaller;
    } catch(JAXBException e) {
      throw new ModelsException(RESTCodes.ModelsErrorCode.MODEL_MARSHALLING_FAILED, Level.INFO,
        "Failed to unmarshal", "Error occurred during unmarshalling setup", e);
    }
  }
  
  private String marshalInt(Object value) throws ModelsException {
    Marshaller marshaller = createMarshaller();
    try(StringWriter sw = new StringWriter()) {
      marshaller.marshal(value, sw);
      return sw.toString();
    } catch (JAXBException | IOException e) {
      throw new ModelsException(RESTCodes.ModelsErrorCode.MODEL_MARSHALLING_FAILED, Level.FINE,
        "Failed to marshal value", "Failed to marshal value:" + value.toString(), e);
    }
  }
  
  public byte[] marshal(Object value) throws ModelsException {
    return marshalInt(value).getBytes(StandardCharsets.UTF_8);
  }
  
  public <O> O unmarshal(String value, Class<O> resultClass) throws ModelsException {
    Unmarshaller unmarshaller = createUnmarshaller();
    try (StringReader sr = new StringReader(value)) {
      StreamSource json = new StreamSource(sr);
      return unmarshaller.unmarshal(json, resultClass).getValue();
    } catch (JAXBException e) {
      throw new ModelsException(RESTCodes.ModelsErrorCode.MODEL_MARSHALLING_FAILED, Level.FINE,
        "Failed to unmarshal value", "Failed to unmarshal value:" + value, e);
    }
  }
  
  public byte[] marshalDescription(ModelDTO modelDTO) throws ModelsException {
    String modelSummaryStr = marshalInt(modelDTO);
    if(modelDTO.getMetrics() != null && !modelDTO.getMetrics().getAttributes().isEmpty()) {
      modelSummaryStr = castMetricsToDouble(modelSummaryStr);
    }
    return modelSummaryStr.getBytes(StandardCharsets.UTF_8);
  }
  
  public ModelDTO unmarshalDescription(String jsonConfig) throws ModelsException {
    return unmarshal(jsonConfig, ModelDTO.class);
  }
  
  private String castMetricsToDouble(String modelSummaryStr) throws ModelsException {
    JSONObject modelSummary = new JSONObject(modelSummaryStr);
    if(modelSummary.has("metrics")) {
      JSONObject metrics = modelSummary.getJSONObject("metrics");
      for(Object metric: metrics.keySet()) {
        String metricKey = null;
        try {
          metricKey = (String) metric;
        } catch (Exception e) {
          throw new ModelsException(RESTCodes.ModelsErrorCode.KEY_NOT_STRING, Level.FINE,
            "keys in metrics dict must be string", e.getMessage(), e);
        }
        try {
          metrics.put(metricKey, Double.valueOf(metrics.getString(metricKey)));
        } catch (Exception e) {
          throw new ModelsException(RESTCodes.ModelsErrorCode.METRIC_NOT_NUMBER, Level.FINE,
            "Provided value for metric " + metricKey + " is not a number" , e.getMessage(), e);
        }
      }
      modelSummary.put("metrics", metrics);
    }
    return modelSummary.toString();
  }
}