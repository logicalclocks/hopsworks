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

package io.hops.hopsworks.api.modelregistry.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.hops.hopsworks.api.modelregistry.models.dto.ModelDTO;
import io.hops.hopsworks.common.util.DtoConverter;
import io.hops.hopsworks.exceptions.ModelRegistryException;
import io.hops.hopsworks.restutils.RESTCodes;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ModelConverter {
  @EJB
  private DtoConverter dtoConverter;
  
  public byte[] marshalDescription(ModelDTO modelDTO) throws ModelRegistryException {
    String modelSummaryStr = writeValue(modelDTO);
    return modelSummaryStr != null ? modelSummaryStr.getBytes(StandardCharsets.UTF_8) : null;
  }
  
  public ModelDTO unmarshalDescription(String jsonConfig) throws ModelRegistryException {
    return readValue(jsonConfig);
  }
  
  private ModelDTO readValue(String jsonConfig) throws ModelRegistryException {
    try {
      return dtoConverter.readValue(jsonConfig, ModelDTO.class);
    } catch (JsonProcessingException e) {
      throw new ModelRegistryException(RESTCodes.ModelRegistryErrorCode.MODEL_MARSHALLING_FAILED, Level.FINE,
        "Failed to unmarshal value", "Failed to unmarshal value:" + jsonConfig, e);
    }
  }
  
  private String writeValue(ModelDTO modelDTO) throws ModelRegistryException {
    String jsonConfig;
    try {
      jsonConfig = dtoConverter.writeValue(modelDTO);
    } catch (JsonProcessingException e) {
      throw new ModelRegistryException(RESTCodes.ModelRegistryErrorCode.MODEL_MARSHALLING_FAILED, Level.FINE,
        "Failed to marshal value", "Failed to unmarshal value:" + modelDTO, e);
    }
    return jsonConfig;
  }
}