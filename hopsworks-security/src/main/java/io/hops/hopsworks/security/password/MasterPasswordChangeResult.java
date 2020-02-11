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

import io.hops.hopsworks.exceptions.EncryptionMasterPasswordException;

public class MasterPasswordChangeResult<R> {
  private final StringBuilder successLog;
  
  private final EncryptionMasterPasswordException cause;
  private final R rollbackItems;
  
  public MasterPasswordChangeResult(StringBuilder successLog) {
    this(successLog, null, null);
  }
  
  public MasterPasswordChangeResult(R rollbackItems, EncryptionMasterPasswordException cause) {
    this(null, rollbackItems, cause);
  }
  
  public MasterPasswordChangeResult(StringBuilder successLog, R rollbackItems,
      EncryptionMasterPasswordException cause) {
    this.successLog = successLog;
    this.rollbackItems = rollbackItems;
    this.cause = cause;
  }
  
  public StringBuilder getSuccessLog() {
    return successLog;
  }
  
  public EncryptionMasterPasswordException getCause() {
    return cause;
  }
  
  public R getRollbackItems() {
    return rollbackItems;
  }
}
