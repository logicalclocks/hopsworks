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

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;

/**
 * Enum representing the type of statistic in the table featurestore_statistic in the hopsworks db
 */
@XmlEnum
public enum FeaturestoreStatisticType {
  @XmlEnumValue("descriptiveStatistics")
  DESCRIPTIVESTATISTICS("descriptiveStatistics"),
  @XmlEnumValue("clusterAnalysis")
  CLUSTERANALYSIS("clusterAnalysis"),
  @XmlEnumValue("featureDistributions")
  FEATUREDISTRIBUTIONS("featureDistributions"),
  @XmlEnumValue("featureCorrelations")
  FEATURECORRELATIONS("featureCorrelations");

  private final String name;

  FeaturestoreStatisticType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
