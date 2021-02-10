/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup.datavalidation;

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
public class RuleAssertionsConverter implements AttributeConverter<Expectation, String> {

  private static final Logger LOGGER = Logger.getLogger(RuleAssertionsConverter.class.getName());

  private static JAXBContext ruleAssertionsJAXBContext;

  static {
    try {
      ruleAssertionsJAXBContext = JAXBContextFactory.createContext(new Class[] {Expectation.class}, null);
    } catch (JAXBException e) {
      LOGGER.log(Level.SEVERE, "An error occurred while initializing JAXBContext", e);
    }
  }

  @Override
  public String convertToDatabaseColumn(Expectation config) {
    try {
      Marshaller marshaller = ruleAssertionsJAXBContext.createMarshaller();
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
  public Expectation convertToEntityAttribute(String jsonConfig) {
    if (Strings.isNullOrEmpty(jsonConfig)) {
      return null;
    }
    try {

      return unmarshal(jsonConfig);
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  private Expectation unmarshal(String jsonConfig) throws JAXBException {
    Unmarshaller unmarshaller = ruleAssertionsJAXBContext.createUnmarshaller();
    StreamSource json = new StreamSource(new StringReader(jsonConfig));
    unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
    unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
    return unmarshaller.unmarshal(json, Expectation.class).getValue();
  }
}
