/*
 * This file is part of Hopsworks
 * Copyright (C) 2024, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.persistence.entity.featurestore.statistics;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.logging.Logger;

@Converter
public class PercentilesConverter implements AttributeConverter<List<Double>, byte[]> {
  private static final Logger LOGGER = Logger.getLogger(PercentilesConverter.class.getName());
  
  @Override
  public byte[] convertToDatabaseColumn(List<Double> percentilesMap) {
    if (percentilesMap == null) {
      return null;
    }
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(percentilesMap);
      oos.flush();
      return bos.toByteArray();
    } catch (IOException e) {
      LOGGER.info("Cannot convert percentiles map to byte array");
    }
    return null;
  }
  
  @Override
  public List<Double> convertToEntityAttribute(byte[] bytes) {
    if (bytes == null) {
      return null;
    }
    try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
         ObjectInputStream ois = new ObjectInputStream(bis)) {
      return (List<Double>) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      LOGGER.info("Cannot convert percentiles map to byte array");
    }
    return null;
  }
}