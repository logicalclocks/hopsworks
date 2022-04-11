/*
 * Copyright (C) 2022, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.serving;

import com.google.common.base.Strings;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

@Converter
public class ComponentResourcesConverter implements AttributeConverter<DeployableComponentResources, String> {
  
  private static final Logger LOGGER = Logger.getLogger(DeployableComponentResources.class.getName());
  private static JAXBContext resourcesJAXBContext;
  
  static {
    try {
      resourcesJAXBContext = JAXBContextFactory.createContext(new Class[] {DeployableComponentResources.class}, null);
    } catch (JAXBException e) {
      LOGGER.log(Level.SEVERE, "An error occurred while initializing JAXBContext", e);
    }
  }
  
  @Override
  public String convertToDatabaseColumn(DeployableComponentResources config) {
    if(config == null) {
      config = new DeployableComponentResources();
    }
    try {
      Marshaller marshaller = resourcesJAXBContext.createMarshaller();
      marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      StringWriter sw = new StringWriter();
      marshaller.marshal(config, sw);
      return sw.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }
  
  @Override
  public DeployableComponentResources convertToEntityAttribute(String jsonConfig) {
    if (Strings.isNullOrEmpty(jsonConfig)) {
      return null;
    }
    try {
      return unmarshal(jsonConfig, resourcesJAXBContext);
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }
  
  private DeployableComponentResources unmarshal(String jsonConfig, JAXBContext jaxbContext) throws JAXBException {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StreamSource json = new StreamSource(new StringReader(jsonConfig));
    unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
    unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
    return unmarshaller.unmarshal(json, DeployableComponentResources.class).getValue();
  }
}
