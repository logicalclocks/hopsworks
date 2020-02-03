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
package io.hops.hopsworks.audit.helper;

public enum AuditAction {
  ACTIVATED_ACCOUNT("ACTIVATED ACCOUNT"),
  VERIFIED_ACCOUNT("VERIFIED ACCOUNT"),
  USER_MANAGEMENT("USER MANAGEMENT"),
  PROFILE_UPDATE("PROFILE UPDATE"),
  LOST_DEVICE("LOST DEVICE REPORT"),
  CHANGED_STATUS("CHANGED STATUS"),
  REGISTRATION("REGISTRATION"),
  SECURITY_QUESTION_CHANGE("SECURITY QUESTION CHANGE"),
  RECOVERY("RECOVERY"),
  QR_CODE("QR CODE"),
  PASSWORD_CHANGE("PASSWORD CHANGE"),
  SECURITY_QUESTION("SECURITY QUESTION RESET"),
  PROFILE("PROFILE UPDATE"),
  PASSWORD("PASSWORD CHANGE"),
  TWO_FACTOR("TWO FACTOR CHANGE"),
  // for adding role by the admin
  ROLE_ADDED("ADDED ROLE"),
  // for removing role by the admin
  ROLE_REMOVED("REMOVED ROLE"),
  ROLE_UPDATED("UPDATED ROLES"),
  LOGIN("LOGIN"),
  LOGOUT("LOGOUT"),
  UNAUTHORIZED("UNAUTHORIZED ACCESS"),
  ALL("ALL");
  
  private final String value;
  
  AuditAction(String value) { this.value = value; }
  
  @Override
  public String toString() { return value; }
}
