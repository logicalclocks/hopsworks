/*
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.persistence.entity.jupyter.config;

import com.google.common.base.Strings;
import io.hops.hopsworks.persistence.entity.jobs.configuration.DockerJobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;
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
public class DockerConfigurationConverter implements AttributeConverter<JobConfiguration, String> {

  private static final Logger LOGGER = Logger.getLogger(DockerConfigurationConverter.class.getName());
  private static JAXBContext dockerJAXBContext;

  static {
    try {
      dockerJAXBContext = JAXBContextFactory.createContext(new Class[] {DockerJobConfiguration.class}, null);
    } catch (JAXBException e) {
      LOGGER.log(Level.SEVERE, "An error occurred while initializing JAXBContext", e);
    }
  }

  @Override
  public String convertToDatabaseColumn(JobConfiguration config) {
    if(config == null) {
      config = new DockerJobConfiguration();
    }
    try {
      Marshaller marshaller = dockerJAXBContext.createMarshaller();
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
  public JobConfiguration convertToEntityAttribute(String jsonConfig) {
    if (Strings.isNullOrEmpty(jsonConfig)) {
      return null;
    }
    try {
      return unmarshal(jsonConfig, dockerJAXBContext);
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  private JobConfiguration unmarshal(String jsonConfig, JAXBContext jaxbContext) throws JAXBException {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StreamSource json = new StreamSource(new StringReader(jsonConfig));
    unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
    unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
    return unmarshaller.unmarshal(json, DockerJobConfiguration.class).getValue();
  }
}
