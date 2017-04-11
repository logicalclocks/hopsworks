package io.hops.hopsworks.kmon.utils;

import java.util.Map;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;

public class CookieUtils {

  public static String read(String name) {
    Map<String, Object> requestCookieMap = FacesContext.
            getCurrentInstance().getExternalContext().getRequestCookieMap();

    Cookie c = (Cookie) requestCookieMap.get(name);

    if (c == null) {
      return "";
    }
    return c.getValue();
  }

  public static void write(String name, String value) {
    FacesContext.getCurrentInstance().getExternalContext()
            .addResponseCookie(name, value, null);
  }
}
