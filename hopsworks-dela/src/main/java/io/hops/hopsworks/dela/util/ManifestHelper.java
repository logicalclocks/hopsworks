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
