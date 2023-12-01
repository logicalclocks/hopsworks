/*
 * This file is part of Hopsworks
 * Copyright (C) 2023, Hopsworks AB. All rights reserved
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

package io.hops.hopsworks.remote.user.oauth2;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import org.junit.Assert;
import org.junit.Test;
import com.nimbusds.openid.connect.sdk.claims.UserInfo;
import net.minidev.json.JSONObject;

public class TestOIDAuthorizationCodeFlowHelper {

  private OIDAuthorizationCodeFlowHelper oidAuthorizationCodeFlowHelper =
      new OIDAuthorizationCodeFlowHelper();

  @Test
  public void testStringClaim_string() throws ParseException {
    String userInfoJson = "{\"sub\":\"sub\", \"claim\": \"claim\"}";
    JSONObject jsonObject = JSONObjectUtils.parse(userInfoJson);
    UserInfo userInfo = new UserInfo(jsonObject);

    String claim = oidAuthorizationCodeFlowHelper.getStringClaim(userInfo, "claim");
    Assert.assertEquals("claim", claim);
  }

  @Test
  public void testStringClaim_array() throws ParseException {
    String userInfoJson = "{\"sub\":\"sub\", \"claim\": [\"claim\"]}";
    JSONObject jsonObject = JSONObjectUtils.parse(userInfoJson);
    UserInfo userInfo = new UserInfo(jsonObject);

    String claim = oidAuthorizationCodeFlowHelper.getStringClaim(userInfo, "claim");
    Assert.assertEquals("claim", claim);
  }

}
