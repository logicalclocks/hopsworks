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
package io.hops.hopsworks.kube.client;

import io.fabric8.kubernetes.api.model.Secret;
import io.hops.hopsworks.kube.client.utils.KubeSettings;

import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Singleton
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class KubeSecretManager {
  @EJB
  private KubeSettings settings;
  @EJB
  private KubeClient kubeClient;
  
  @Lock(LockType.READ)
  public String getMasterEncryptionPassword() {
    Secret secret = kubeClient.getSecret(settings.get(KubeSettings.KubeSettingKeys.HOPSWORKS_SECRET));
    return secret.getData().get(settings.get(KubeSettings.KubeSettingKeys.MASTER_ENCRYPTION_PASSWORD));
  }
  
  public void patchMasterEncryptionPassword(String sha256HexPassword) {
    Secret secret = kubeClient.getSecret(settings.get(KubeSettings.KubeSettingKeys.HOPSWORKS_SECRET));
    secret.getData().put(settings.get(KubeSettings.KubeSettingKeys.MASTER_ENCRYPTION_PASSWORD), sha256HexPassword);
    kubeClient.patchSecret(settings.get(KubeSettings.KubeSettingKeys.HOPSWORKS_SECRET), secret);
  }
}
