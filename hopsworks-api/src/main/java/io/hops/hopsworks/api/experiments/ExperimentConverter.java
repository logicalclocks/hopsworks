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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.hops.hopsworks.api.experiments.dto.ExperimentDTO;
import io.hops.hopsworks.api.experiments.dto.results.ExperimentResultSummaryDTO;
import io.hops.hopsworks.exceptions.ExperimentsException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class ExperimentConverter {
  
  private <T> T readValue(String jsonConfig, Class<T> resultClass) throws ExperimentsException {
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      return objectMapper.readValue(jsonConfig, resultClass);
    } catch (JsonProcessingException e) {
      throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.EXPERIMENT_MARSHALLING_FAILED, Level.FINE,
        "Failed to unmarshal", "Error occurred during unmarshalling:" + jsonConfig, e);
    }
  }
  
  private String writeValue(Object value) throws ExperimentsException {
    String jsonConfig;
    ObjectMapper objectMapper = new ObjectMapper();
    try {
      jsonConfig = objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new ExperimentsException(RESTCodes.ExperimentsErrorCode.EXPERIMENT_MARSHALLING_FAILED, Level.FINE,
        "Failed to marshal", "Failed to marshal:" + value.toString(), e);
    }
    return jsonConfig;
  }
  
  public byte[] marshal(Object value) throws ExperimentsException {
    String sw = writeValue(value);
    return sw != null ? sw.getBytes(StandardCharsets.UTF_8) : null;
  }
  
  public <O> O unmarshal(String value, Class<O> resultClass) throws ExperimentsException {
    return readValue(value, resultClass);
  }
  
  public ExperimentDTO unmarshalDescription(String jsonConfig) throws ExperimentsException {
    return unmarshal(jsonConfig, ExperimentDTO.class);
  }
  
  public ExperimentResultSummaryDTO unmarshalResults(String jsonResults) throws ExperimentsException {
    return unmarshal(jsonResults, ExperimentResultSummaryDTO.class);
  }
}