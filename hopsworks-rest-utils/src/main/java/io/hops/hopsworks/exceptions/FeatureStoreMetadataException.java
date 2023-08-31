/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.exceptions;

import io.hops.hopsworks.restutils.RESTCodes;
import io.hops.hopsworks.restutils.RESTException;

import java.util.logging.Level;

public class FeatureStoreMetadataException extends RESTException {
  public FeatureStoreMetadataException(RESTCodes.SchematizedTagErrorCode errorCode, Level level) {
    super(errorCode, level);
  }
  
  public FeatureStoreMetadataException(RESTCodes.SchematizedTagErrorCode errorCode, Level level, String usrMsg) {
    super(errorCode, level, usrMsg);
  }
  
  public FeatureStoreMetadataException(RESTCodes.SchematizedTagErrorCode errorCode, Level level, String usrMsg,
                                       String devMsg) {
    super(errorCode, level, usrMsg, devMsg);
  }
  
  public FeatureStoreMetadataException(RESTCodes.SchematizedTagErrorCode errorCode, Level level, String usrMsg,
                                       String devMsg, Throwable throwable) {
    super(errorCode, level, usrMsg, devMsg, throwable);
  }
  
  public static FeatureStoreMetadataException schemaNotDefined(String name, Level lvl) {
    return new FeatureStoreMetadataException(RESTCodes.SchematizedTagErrorCode.TAG_SCHEMA_NOT_FOUND,
      lvl, "schema: " + name + " is not defined");
  }
}
