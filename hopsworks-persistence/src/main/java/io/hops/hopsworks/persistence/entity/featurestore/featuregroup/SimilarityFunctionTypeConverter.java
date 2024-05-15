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

package io.hops.hopsworks.persistence.entity.featurestore.featuregroup;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class SimilarityFunctionTypeConverter implements AttributeConverter<SimilarityFunctionType, String> {
  @Override
  public String convertToDatabaseColumn(SimilarityFunctionType attribute) {
    return attribute.name().toLowerCase(); // Convert enum value to lowercase string
  }

  @Override
  public SimilarityFunctionType convertToEntityAttribute(String dbData) {
    return SimilarityFunctionType.valueOf(dbData.toUpperCase()); // Convert lowercase string to uppercase enum value
  }
}
