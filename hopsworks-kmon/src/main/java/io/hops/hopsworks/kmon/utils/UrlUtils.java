package io.hops.hopsworks.kmon.utils;

import java.util.HashMap;
import java.util.Map;

public class UrlUtils {

  public static String addParams(String url, HashMap<String, String> params) {
    for (Map.Entry<String, String> entry : params.entrySet()) {
      url += entry.getKey() + "=" + entry.getValue() + "&";
    }
    return url;
  }
}
