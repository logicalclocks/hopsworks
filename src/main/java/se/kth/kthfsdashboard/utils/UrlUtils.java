package se.kth.kthfsdashboard.utils;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class UrlUtils {

   public static String addParams(String url, HashMap<String, String> params) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
         url += entry.getKey() + "=" + entry.getValue() + "&";
      }
      return url;
   }
}
