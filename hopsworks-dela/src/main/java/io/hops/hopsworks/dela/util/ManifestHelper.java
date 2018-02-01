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

package io.hops.hopsworks.dela.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.hops.hopsworks.dela.exception.ThirdPartyException;
import io.hops.hopsworks.dela.old_dto.ManifestJSON;
import java.io.UnsupportedEncodingException;
import javax.ws.rs.core.Response;

public class ManifestHelper {
  public static byte[] marshall(ManifestJSON manifest) throws ThirdPartyException {
    Gson gson = new GsonBuilder().create();
    String jsonString = gson.toJson(manifest);
    byte[] jsonByte;
    try {
      jsonByte = jsonString.getBytes("UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "manifest cannot be read as UTF-8",
        ThirdPartyException.Source.LOCAL, "error");
    }
    return jsonByte;
  }

  public static ManifestJSON unmarshall(byte[] jsonByte) throws ThirdPartyException {
    String jsonString;
    try {
      jsonString = new String(jsonByte, "UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new ThirdPartyException(Response.Status.BAD_REQUEST.getStatusCode(), "manifest cannot be read as UTF-8",
        ThirdPartyException.Source.LOCAL, "error");
    }
    ManifestJSON manifest = new Gson().fromJson(jsonString, ManifestJSON.class);
    return manifest;
  }
}
