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

package io.hops.hopsworks.persistence.entity.jupyter.config;

import com.google.common.base.Strings;
import io.hops.hopsworks.persistence.entity.jobs.configuration.JobConfiguration;
import io.hops.hopsworks.persistence.entity.jobs.configuration.spark.SparkJobConfiguration;
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
public class JupyterConfigurationConverter implements AttributeConverter<JobConfiguration, String> {

  private static final Logger LOGGER = Logger.getLogger(JupyterConfigurationConverter.class.getName());
  private static JAXBContext jobJAXBContext;

  static {
    try {
      jobJAXBContext = JAXBContextFactory.createContext(new Class[] {SparkJobConfiguration.class}, null);
    } catch (JAXBException e) {
      LOGGER.log(Level.SEVERE, "An error occurred while initializing JAXBContext", e);
    }
  }

  @Override
  public String convertToDatabaseColumn(JobConfiguration config) {
    if(config == null) {
      config = new SparkJobConfiguration();
    }
    try {
      Marshaller marshaller = jobJAXBContext.createMarshaller();
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
      return unmarshal(jsonConfig, jobJAXBContext);
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  private JobConfiguration unmarshal(String jsonConfig, JAXBContext jaxbContext) throws JAXBException {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StreamSource json = new StreamSource(new StringReader(jsonConfig));
    unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
    unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
    return unmarshaller.unmarshal(json, SparkJobConfiguration.class).getValue();
  }
}
