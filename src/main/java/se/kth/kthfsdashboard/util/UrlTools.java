/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.util;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class UrlTools {

   public static String addParams(String url, HashMap<String, String> params) {
      for (Map.Entry<String, String> entry : params.entrySet()) {
         url += entry.getKey() + "=" + entry.getValue() + "&";
      }
      return url;
   }
}
