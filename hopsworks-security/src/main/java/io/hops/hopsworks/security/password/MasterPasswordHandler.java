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

package io.hops.hopsworks.security.password;

import io.hops.hopsworks.security.encryption.SymmetricEncryptionUtil;

/**
 * Interface for various handlers when Hopsworks master encryption password changes
 */
public interface MasterPasswordHandler {
  void pre();
  MasterPasswordChangeResult perform(String oldPassword, String newPassword);
  void rollback(MasterPasswordChangeResult result);
  void post();
  default String getNewUserPassword(String userPassword, String cipherText, String oldMasterPassword,
    String newMasterPassword) throws Exception {
    String plainCertPassword = SymmetricEncryptionUtil.decrypt(userPassword, cipherText, oldMasterPassword);
    return SymmetricEncryptionUtil.encrypt(userPassword, plainCertPassword, newMasterPassword);
  }
}
