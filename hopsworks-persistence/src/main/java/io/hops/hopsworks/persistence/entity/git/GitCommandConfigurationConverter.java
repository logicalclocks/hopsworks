package io.hops.hopsworks.persistence.entity.git;

import io.hops.hopsworks.persistence.entity.git.config.GitCommandConfiguration;

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
public class GitCommandConfigurationConverter implements AttributeConverter<GitCommandConfiguration, String> {
  private static final Logger LOGGER = Logger.getLogger(GitCommandConfiguration.class.getName());

  private static JAXBContext commandConfigurationContext;

  static {
    try {
      commandConfigurationContext = JAXBContextFactory.createContext(new Class[] {GitCommandConfiguration.class},
          null);
    } catch (JAXBException e) {
      LOGGER.log(Level.SEVERE, "An error occurred while initializing JAXBContext", e);
    }
  }

  @Override
  public String convertToDatabaseColumn(GitCommandConfiguration commandConfiguration) {
    try {
      Marshaller marshaller = commandConfigurationContext.createMarshaller();
      marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      StringWriter sw = new StringWriter();
      marshaller.marshal(commandConfiguration, sw);
      return sw.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public GitCommandConfiguration convertToEntityAttribute(String jsonConfig) {
    if (Strings.isNullOrEmpty(jsonConfig)) {
      return null;
    }
    try {
      Unmarshaller unmarshaller = commandConfigurationContext.createUnmarshaller();
      StreamSource json = new StreamSource(new StringReader(jsonConfig));
      unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      return unmarshaller.unmarshal(json, GitCommandConfiguration.class).getValue();
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }
}
