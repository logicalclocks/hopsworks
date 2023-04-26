/*
 * This file is part of Hopsworks
 * Copyright (C) 2022, Hopsworks AB. All rights reserved
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
package io.hops.hopsworks.ca.configuration;

import io.hops.hadoop.shaded.com.google.gson.annotations.SerializedName;

import java.util.Optional;

public class CAsConfiguration {

  @SerializedName("rootCA")
  final private CAConfiguration rootCA;

  @SerializedName("intermediateCA")
  final private IntermediateCAConfiguration intermediateCA;

  @SerializedName("kubernetesCA")
  final private KubeCAConfiguration kubernetesCA;

  public CAsConfiguration(CAConfiguration rootCA, IntermediateCAConfiguration intermediateCA,
      KubeCAConfiguration kubernetesCA) {
    this.rootCA = rootCA;
    this.intermediateCA = intermediateCA;
    this.kubernetesCA = kubernetesCA;
  }

  public Optional<CAConfiguration> getRootCA() {
    return Optional.ofNullable(rootCA);
  }

  public Optional<IntermediateCAConfiguration> getIntermediateCA() {
    return Optional.ofNullable(intermediateCA);
  }

  public Optional<CAConfiguration> getKubernetesCA() {
    return Optional.ofNullable(kubernetesCA);
  }
}
