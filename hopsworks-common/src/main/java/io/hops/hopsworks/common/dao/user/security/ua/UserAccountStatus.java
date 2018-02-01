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

package io.hops.hopsworks.common.dao.user.security.ua;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType(name = "userAccountStatus")
@XmlEnum
public enum UserAccountStatus {

  // Status of new Mobile users requests
  @XmlEnumValue("NEW_MOBILE_ACCOUNT")
  NEW_MOBILE_ACCOUNT(0),

  // For new account requests where users should validate their account request
  @XmlEnumValue("VERIFIED_ACCOUNT")
  VERIFIED_ACCOUNT(1),

  // When user is approved to use the platform
  @XmlEnumValue("ACTIVATED_ACCOUNT")
  ACTIVATED_ACCOUNT(2),

  // Users that are no longer granted to access the platform.
  // Users with this state, can not login, change password even as guest users
  @XmlEnumValue("DEACTIVATED_ACCOUNT")
  DEACTIVATED_ACCOUNT(3),

  // Blocked users can not user the resources. This can be due to suspicious behaviour of users. 
  // For example. multiple false logings
  @XmlEnumValue("BLOCKED_ACCOUNT")
  BLOCKED_ACCOUNT(4),

  // For scenrios where mobile device is compromised/lost
  @XmlEnumValue("LOST_MOBILE")
  LOST_MOBILE(5),

  // Mark account as SPAM
  @XmlEnumValue("SPAM_ACCOUNT")
  SPAM_ACCOUNT(6);

  private final int value;

  private UserAccountStatus(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static UserAccountStatus fromValue(int v) {
    for (UserAccountStatus c : UserAccountStatus.values()) {
      if (c.value == v) {
        return c;
      }
    }
    throw new IllegalArgumentException("" + v);
  }
}
