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

package io.hops.hopsworks.common.jobs.configuration;

import com.google.common.base.Strings;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import io.hops.hopsworks.common.jobs.flink.FlinkJobConfiguration;
import io.hops.hopsworks.common.jobs.jobhistory.JobType;
import io.hops.hopsworks.common.jobs.spark.SparkJobConfiguration;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.json.JSONObject;

@Converter
public class JobConfigurationConverter implements AttributeConverter<JobConfiguration, String> {

  private static final Logger LOGGER = Logger.getLogger(JobConfigurationConverter.class.getName());


  private static JAXBContext sparkJAXBContext;
  private static JAXBContext flinkJAXBContext;

  static {
    try {
      sparkJAXBContext = JAXBContextFactory.createContext(new Class[] {SparkJobConfiguration.class}, null);
      flinkJAXBContext = JAXBContextFactory.createContext(new Class[] {FlinkJobConfiguration.class}, null);
    } catch (JAXBException e) {
      LOGGER.log(Level.SEVERE, "An error occurred while initializing JAXBContext", e);
    }
  }

  @Override
  public String convertToDatabaseColumn(JobConfiguration config) {
    try {
      JAXBContext jc = getJAXBContext(config.getJobType());
      Marshaller marshaller = jc.createMarshaller();
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
      JSONObject obj = new JSONObject(jsonConfig);
      JobType type = JobType.valueOf((((String)obj.get("jobType"))));
      JAXBContext jc = getJAXBContext(type);
      return unmarshal(jsonConfig, type, jc);
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  private JAXBContext getJAXBContext(JobType jobType) {
    switch(jobType) {
      case SPARK:
      case PYSPARK:
        return sparkJAXBContext;
      case FLINK:
        return flinkJAXBContext;
      default:
        throw new IllegalArgumentException("Could not find a mapping for JobType " + jobType);
    }
  }

  private JobConfiguration unmarshal(String jsonConfig, JobType jobType, JAXBContext jaxbContext) throws JAXBException {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StreamSource json = new StreamSource(new StringReader(jsonConfig));
    unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
    unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
    switch(jobType) {
      case SPARK:
      case PYSPARK:
        return unmarshaller.unmarshal(json, SparkJobConfiguration.class).getValue();
      case FLINK:
        return unmarshaller.unmarshal(json, FlinkJobConfiguration.class).getValue();
      default:
        throw new IllegalArgumentException("Could not find a mapping for JobType " + jobType);
    }
  }
}
