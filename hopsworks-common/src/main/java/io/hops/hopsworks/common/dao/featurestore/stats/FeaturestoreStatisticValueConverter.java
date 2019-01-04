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

package io.hops.hopsworks.common.dao.featurestore.stats;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.featurestore.stats.cluster_analysis.ClusterAnalysisDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.desc_stats.DescriptiveStatsMetricValuesDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_correlation.CorrelationValueDTO;
import io.hops.hopsworks.common.dao.featurestore.stats.feature_distributions.FeatureDistributionDTO;
import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.json.JSONObject;

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

/**
 * Different statistics are stored in different JSON format in the table featurestore_statistics. This converter is
 * used to automatically convert to/from the JSON in to the correct entity.
 */
@Converter
public class FeaturestoreStatisticValueConverter implements AttributeConverter<FeaturestoreStatisticValue, String> {

  private static final Logger LOGGER = Logger.getLogger(FeaturestoreStatisticValueConverter.class.getName());
  private static JAXBContext descriptiveStatsJAXBContext;
  private static JAXBContext featureCorrelationJAXBContext;
  private static JAXBContext featureHistogramsJAXBContext;
  private static JAXBContext clusterAnalysisJAXBContext;

  static {
    try {
      descriptiveStatsJAXBContext =
          JAXBContextFactory.createContext(new Class[]{DescriptiveStatsMetricValuesDTO.class}, null);
      featureCorrelationJAXBContext =
          JAXBContextFactory.createContext(new Class[]{CorrelationValueDTO.class}, null);
      featureHistogramsJAXBContext =
          JAXBContextFactory.createContext(new Class[]{FeatureDistributionDTO.class}, null);
      clusterAnalysisJAXBContext =
          JAXBContextFactory.createContext(new Class[]{ClusterAnalysisDTO.class}, null);
    } catch (JAXBException e) {
      LOGGER.log(Level.SEVERE, "An error occurred while initializing JAXBContext", e);
    }
  }

  /**
   * Method used for converting the entity into JSON to store in the database
   *
   * @param featurestoreStatisticValue the entity to convert
   * @return JSON string of the entity
   */
  @Override
  public String convertToDatabaseColumn(FeaturestoreStatisticValue featurestoreStatisticValue) {
    try {
      JAXBContext jc = getJAXBContext(featurestoreStatisticValue.getStatisticType());
      Marshaller marshaller = jc.createMarshaller();
      marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
      marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
      StringWriter sw = new StringWriter();
      marshaller.marshal(featurestoreStatisticValue, sw);
      return sw.toString();
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Method used for converting serialized JSON representation of a statisticValue into a java entity
   *
   * @param featurestoreStatisticValueJsonStr the JSON string to convert
   * @return a FeatureStoreStatisticValue entity with the JSON data
   */
  @Override
  public FeaturestoreStatisticValue convertToEntityAttribute(String featurestoreStatisticValueJsonStr) {
    if (Strings.isNullOrEmpty(featurestoreStatisticValueJsonStr)) {
      return null;
    }
    try {
      JSONObject featurestoreStatisticValueJsonObj = new JSONObject(featurestoreStatisticValueJsonStr);
      FeaturestoreStatisticType featurestoreStatisticType =
          FeaturestoreStatisticType.valueOf(
              (((String) featurestoreStatisticValueJsonObj.get("statisticType")).toUpperCase()));
      JAXBContext jc = getJAXBContext(featurestoreStatisticType);
      return unmarshal(featurestoreStatisticValueJsonStr, featurestoreStatisticType, jc);
    } catch (JAXBException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Returns the correct JAXBContext for marshalling/unmarshalling depending on the statistictype
   *
   * @param featurestoreStatisticType the type of the statistic
   * @return the JAXBContext
   * @throws JAXBException
   */
  private JAXBContext getJAXBContext(FeaturestoreStatisticType featurestoreStatisticType) {
    switch (featurestoreStatisticType) {
      case DESCRIPTIVESTATISTICS:
        return descriptiveStatsJAXBContext;
      case FEATURECORRELATIONS:
        return featureCorrelationJAXBContext;
      case FEATUREDISTRIBUTIONS:
        return featureHistogramsJAXBContext;
      case CLUSTERANALYSIS:
        return clusterAnalysisJAXBContext;
      default:
        throw new IllegalArgumentException("Could not find a mapping for FeaturestoreStatisticType " +
            featurestoreStatisticType);
    }
  }

  /**
   * Method for unmarshalling JSON stored in the database into a suitable entity
   *
   * @param featurestoreStatisticValueJson the json to unmarshal
   * @param featurestoreStatisticType the type of the statistic stored in the JSON
   * @param jaxbContext the JAXBContext that defines the JSON structure
   * @return
   * @throws JAXBException
   */
  private FeaturestoreStatisticValue unmarshal(String featurestoreStatisticValueJson,
                                               FeaturestoreStatisticType featurestoreStatisticType,
                                               JAXBContext jaxbContext) throws JAXBException {
    Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
    StreamSource json = new StreamSource(new StringReader(featurestoreStatisticValueJson));
    unmarshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, false);
    unmarshaller.setProperty(MarshallerProperties.MEDIA_TYPE, MediaType.APPLICATION_JSON);
    switch (featurestoreStatisticType) {
      case DESCRIPTIVESTATISTICS:
        return unmarshaller.unmarshal(json, DescriptiveStatsMetricValuesDTO.class).getValue();
      case FEATURECORRELATIONS:
        return unmarshaller.unmarshal(json, CorrelationValueDTO.class).getValue();
      case FEATUREDISTRIBUTIONS:
        return unmarshaller.unmarshal(json, FeatureDistributionDTO.class).getValue();
      case CLUSTERANALYSIS:
        return unmarshaller.unmarshal(json, ClusterAnalysisDTO.class).getValue();
      default:
        throw new IllegalArgumentException("Could not find a mapping for featurestore statistic type "
            + featurestoreStatisticType);
    }
  }
}
