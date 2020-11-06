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

package io.hops.hopsworks.api.experiments;

import io.hops.hopsworks.api.experiments.dto.ExperimentDTO;
import io.hops.hopsworks.api.experiments.dto.results.ExperimentResultSummaryDTO;
import io.hops.hopsworks.exceptions.ExperimentsException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;

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
public class ExperimentConverter {
  
  private static JAXBContext jaxbExperimentContext;
  
  @PostConstruct
  public void init() {
    try {
      Class[] serializedClasses = new Class[]{
        ExperimentDTO.class,
        ModelXAttr.class,
        ExperimentResultSummaryDTO.class
      };
      jaxbExperimentContext = JAXBContextFactory.createContext(serializedClasses, null);
    } catch (JAXBException e) {
      e.printStackTrace();
    }
  }
  
  private Marshaller createMarshaller() throws ExperimentsException {
    try {
      Marshaller marshaller = jaxbExperimentContext.createMarshaller();
      marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      return marshaller;
    } catch(JAXBException e) {
      throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.EXPERIMENT_MARSHALLING_FAILED, Level.INFO,
        "Failed to unmarshal", "Error occurred during unmarshalling setup", e);
    }
  }
  
  private Unmarshaller createUnmarshaller() throws ExperimentsException {
    try {
      Unmarshaller unmarshaller = jaxbExperimentContext.createUnmarshaller();
      unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      return unmarshaller;
    } catch(JAXBException e) {
      throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.EXPERIMENT_MARSHALLING_FAILED, Level.INFO,
        "Failed to unmarshal", "Error occurred during unmarshalling setup", e);
    }
  }
  
  public byte[] marshal(Object value) throws ExperimentsException {
    Marshaller marshaller = createMarshaller();
    try(StringWriter sw = new StringWriter()) {
      marshaller.marshal(value, sw);
      return sw.toString().getBytes(StandardCharsets.UTF_8);
    } catch (JAXBException | IOException e) {
      throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.EXPERIMENT_MARSHALLING_FAILED, Level.FINE,
        "Failed to marshal", "Failed to marshal:" + value.toString(), e);
    }
  }
  
  public <O> O unmarshal(String value, Class<O> resultClass) throws ExperimentsException {
    Unmarshaller unmarshaller = createUnmarshaller();
    try (StringReader sr = new StringReader(value)) {
      StreamSource json = new StreamSource(sr);
      return unmarshaller.unmarshal(json, resultClass).getValue();
    } catch (JAXBException e) {
      throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.EXPERIMENT_MARSHALLING_FAILED, Level.FINE,
        "Failed to unmarshal", "Error occurred during unmarshalling:" + value, e);
    }
  }
  
  public ExperimentDTO unmarshalDescription(String jsonConfig) throws ExperimentsException {
    return unmarshal(jsonConfig, ExperimentDTO.class);
  }
  
  public ExperimentResultSummaryDTO unmarshalResults(String jsonResults) throws ExperimentsException {
    return unmarshal(jsonResults, ExperimentResultSummaryDTO.class);
  }
}