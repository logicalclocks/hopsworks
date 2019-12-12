/*
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
 */
package io.hops.hopsworks.common.util;

import io.hops.hopsworks.common.provenance.core.dto.ProvCoreDTO;
import io.hops.hopsworks.common.provenance.core.dto.ProvFeatureDTO;
import io.hops.hopsworks.common.provenance.core.dto.ProvTypeDTO;
import io.hops.hopsworks.exceptions.GenericException;
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
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class HopsworksJAXBContext {
  private static JAXBContext context;
  
  @PostConstruct
  public void init() {
    try {
      Map<String, Object> properties = new HashMap<>();
      properties.put(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      properties.put(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      context = JAXBContextFactory.
        createContext(new Class[] {
          ProvCoreDTO.class,
          ProvTypeDTO.class,
          ProvFeatureDTO.class
        }, properties);
    } catch (JAXBException e) {
      e.printStackTrace();
    }
  }
  
  public <V> String marshal(V obj) throws GenericException {
    try {
      Marshaller marshaller = context.createMarshaller();
      StringWriter sw = new StringWriter();
      marshaller.marshal(obj, sw);
      return sw.toString();
    } catch(JAXBException e) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, Level.INFO, "jaxb marshal exception");
    }
  }
  
  public <V> V unmarshal(String json, Class<V> type) throws GenericException {
    try {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      StreamSource ss = new StreamSource(new StringReader(json));
      return unmarshaller.unmarshal(ss, type).getValue();
    } catch(JAXBException e) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, Level.INFO, "jaxb unmarshall exception");
    }
  }

  public <V> List<V> unmarshalList(String json, Class<V> type) throws GenericException {
    try {
      Unmarshaller unmarshaller = context.createUnmarshaller();
      StreamSource ss = new StreamSource(new StringReader(json));
      JAXBElement<V> e = unmarshaller.unmarshal(ss, type);
      return (List<V>)e.getValue(); //this cast is mainly because of weird behaviour of jaxb combined with java generics
    } catch(JAXBException e) {
      throw new GenericException(RESTCodes.GenericErrorCode.ILLEGAL_STATE, Level.INFO, "jaxb unmarshall exception");
    }
  }
}
