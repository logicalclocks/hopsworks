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

package io.hops.hopsworks.common.dao.user.security.audit;

public enum AccountsAuditActions {
  USERMANAGEMENT("USER MANAGEMENT"),
  PROFILEUPDATE("PROFILE UPDATE"),
  LOSTDEVICE("LOST DEVICE REPORT"),
  CHANGEDSTATUS("CHANGED STATUS"),
  REGISTRATION("REGISTRATION"),
  SECQUESTIONCHANGE("SECURITY QUESTION CHANGE"),
  UNREGISTRATION("UNREGISTRATION"),
  RECOVERY("RECOVERY"),
  QRCODE("QR CODE"),
  PASSWORDCHANGE("PASSWORD CHANGE"),
  SECQUESTION("SECURTY QUESTION RESET"),
  PROFILE("PROFILE UPDATE"),
  PASSWORD("PASSWORD CHANGE"),
  TWO_FACTOR("TWO FACTOR CHANGE"),
  ABORTED("ABORTED"),
  FAILED("FAILED"),
  SUCCESS("SUCCESS"),
  ALL("ALL");

  private final String value;

  AccountsAuditActions(String value) { this.value = value; }

  @Override
  public String toString() { return value; }
}
