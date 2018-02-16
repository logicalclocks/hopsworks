/*
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
 *
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
