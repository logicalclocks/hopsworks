/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.jobs.configuration;

import com.google.common.base.Strings;
import io.hops.hopsworks.common.dao.jobs.JsonReduceable;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import io.hops.hopsworks.common.jobs.MutableJsonObject;

@Converter
public class JsonReduceableConverter implements
        AttributeConverter<JsonReduceable, String> {

  @Override
  public String convertToDatabaseColumn(JsonReduceable config) {
    return config.getReducedJsonObject().toJson();
  }

  @Override
  public JobConfiguration convertToEntityAttribute(String config) {
    if (Strings.isNullOrEmpty(config)) {
      return null;
    }
    try (JsonReader reader = Json.createReader(new StringReader(config))) {
      JsonObject obj = reader.readObject();
      MutableJsonObject json = new MutableJsonObject(obj);
      return JobConfiguration.JobConfigurationFactory.
              getJobConfigurationFromJson(json);
    }
  }

}
