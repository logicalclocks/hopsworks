/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.featurestore.storageconnectors.kafka;

import java.io.Serializable;

public class FeatureStoreKafkaConnectorSecrets implements Serializable {
  private String sslTrustStorePassword;
  private String sslKeyStorePassword;
  private String sslKeyPassword;
  
  public FeatureStoreKafkaConnectorSecrets() {}

  public FeatureStoreKafkaConnectorSecrets(String sslTrustStorePassword, String sslKeyStorePassword,
                                           String sslKeyPassword) {
    this.sslTrustStorePassword = sslTrustStorePassword;
    this.sslKeyStorePassword = sslKeyStorePassword;
    this.sslKeyPassword = sslKeyPassword;
  }
  
  public String getSslTrustStorePassword() {
    return sslTrustStorePassword;
  }
  
  public void setSslTrustStorePassword(String sslTrustStorePassword) {
    this.sslTrustStorePassword = sslTrustStorePassword;
  }
  
  public String getSslKeyStorePassword() {
    return sslKeyStorePassword;
  }
  
  public void setSslKeyStorePassword(String sslKeyStorePassword) {
    this.sslKeyStorePassword = sslKeyStorePassword;
  }
  
  public String getSslKeyPassword() {
    return sslKeyPassword;
  }
  
  public void setSslKeyPassword(String sslKeyPassword) {
    this.sslKeyPassword = sslKeyPassword;
  }
  
  @Override
  public String toString() {
    return "FeatureStoreKafkaConnectorSecrets{" +
      "sslTrustStorePassword='" + sslTrustStorePassword + '\'' +
      ", sslKeyStorePassword='" + sslKeyStorePassword + '\'' +
      ", sslKeyPassword='" + sslKeyPassword + '\'' +
      '}';
  }
}
