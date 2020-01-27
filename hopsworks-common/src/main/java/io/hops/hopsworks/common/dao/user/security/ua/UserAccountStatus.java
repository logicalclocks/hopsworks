/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
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

  // For scenarios where mobile device is compromised/lost
  @XmlEnumValue("LOST_MOBILE")
  LOST_MOBILE(5),

  // Mark account as SPAM
  @XmlEnumValue("SPAM_ACCOUNT")
  SPAM_ACCOUNT(6),
  
  // Mark account as temp password
  @XmlEnumValue("TEMP_PASSWORD")
  TEMP_PASSWORD(7);

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
  
  public String getUserStatus() {
    switch (this) {
      case SPAM_ACCOUNT:
        return "Spam account";
      case DEACTIVATED_ACCOUNT:
        return "Deactivated account";
      case VERIFIED_ACCOUNT:
        return "Verified account";
      case ACTIVATED_ACCOUNT:
        return "Activated account";
      case NEW_MOBILE_ACCOUNT:
        return "New account";
      case BLOCKED_ACCOUNT:
        return "Blocked account";
      case LOST_MOBILE:
        return "Lost mobile account";
      case TEMP_PASSWORD:
        return "Temporary password";
      default:
        return "Unknown account type";
    }
  }
}
