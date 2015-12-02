/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.audit;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import javax.faces.FacesException;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

 
public class AuditUtil {

  /**
   * Get the user IP address.
   * @return 
   */
  public static String getIPAddress() {
    HttpServletRequest httpServletRequest = (HttpServletRequest) FacesContext.
            getCurrentInstance().getExternalContext().getRequest();
    return httpServletRequest.getRemoteAddr();
  }

  /**
   * Get the user IP address.
   * @param req
   * @return 
   */
   public static String getIPAddress(HttpServletRequest req) {
    
    return req.getRemoteHost();
  }
  /**
   * Get the user operating system info.
   * @param req
   * @return 
   */
  public static String getOSInfo(HttpServletRequest req) {

    String userAgent = req.getHeader("User-Agent");
    
    String os = null;

    // Find the OS info
    if (userAgent.toLowerCase().contains("windows")) {
      os = "Windows";
    } else if (userAgent.toLowerCase().contains("mac")) {
      os = "Mac";
    } else if (userAgent.toLowerCase().contains("x11")) {
      os = "Unix";
    } else if (userAgent.toLowerCase().contains("android")) {
      os = "Android";
    } else if (userAgent.toLowerCase().contains("iphone")) {
      os = "IPhone";
    } else {
      os = "UnKnown, More-Info: " + userAgent;
    }
    return os;
  }

    /**
   * Get the user operating system info.
   * @return 
   */
  public static String getOSInfo() {

    ExternalContext externalContext = FacesContext.getCurrentInstance().
            getExternalContext();

    String userAgent = externalContext.getRequestHeaderMap().get("User-Agent");

    String os = null;

    // Find the OS info
    if (userAgent.toLowerCase().contains("windows")) {
      os = "Windows";
    } else if (userAgent.toLowerCase().contains("mac")) {
      os = "Mac";
    } else if (userAgent.toLowerCase().contains("x11")) {
      os = "Unix";
    } else if (userAgent.toLowerCase().contains("android")) {
      os = "Android";
    } else if (userAgent.toLowerCase().contains("iphone")) {
      os = "IPhone";
    } else {
      os = "UnKnown, More-Info: " + userAgent;
    }
    return os;
  }
  /**
   * Get the user's browser info.
   * @return 
   */
  public static String getBrowserInfo() {

    ExternalContext externalContext = FacesContext.getCurrentInstance().
            getExternalContext();

    String userAgent = externalContext.getRequestHeaderMap().get("User-Agent");

    String browser = null;

    // Find the browser info
    if (userAgent.toLowerCase().contains("msie")) {
      browser = "Internet Explorer";
    } else if (userAgent.toLowerCase().contains("firefox")) {
      browser = "Firefox";
    } else if (userAgent.toLowerCase().contains("chrome")) {
      browser = "Chrome";
    } else if (userAgent.toLowerCase().contains("opera")) {
      browser = "Opera";
    } else if (userAgent.toLowerCase().contains("safari")) {
      browser = "Safari";
    } else {
      browser = "UnKnown, More-Info: " + userAgent;
    }

    return browser;
  }

    /**
   * Get the user's browser info.
   * @return 
   */
  public static String getBrowserInfo(HttpServletRequest req){

    String userAgent = req.getHeader("User-Agent");
    String browser = null;

    // Find the browser info
    if (userAgent.toLowerCase().contains("msie")) {
      browser = "Internet Explorer";
    } else if (userAgent.toLowerCase().contains("firefox")) {
      browser = "Firefox";
    } else if (userAgent.toLowerCase().contains("chrome")) {
      browser = "Chrome";
    } else if (userAgent.toLowerCase().contains("opera")) {
      browser = "Opera";
    } else if (userAgent.toLowerCase().contains("safari")) {
      browser = "Safari";
    } else {
      browser = "UnKnown, More-Info: " + userAgent;
    }

    return browser;
  }
  /**
   * Get the user MAC address.
   * @param ip
   * @return
   * @throws SocketException 
   */
  public static String getMacAddress(String ip) throws SocketException {

    // Get the mac
    String macAddress = null;

    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.
            getNetworkInterfaces();

    while (networkInterfaces.hasMoreElements()) {

      NetworkInterface network = networkInterfaces.nextElement();

      byte[] mac = network.getHardwareAddress();

      if (mac != null) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mac.length; i++) {
          sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-"
                  : ""));
        }
        macAddress = sb.toString();
        break;
      }
    }
    return macAddress;
  }

  
  public static String getUserURL(HttpServletRequest req){
  
    String domain = req.getRequestURL().toString();
    String cpath = req.getContextPath().toString();

    String url = domain.substring(0, domain.indexOf(cpath));
    return url + cpath;
  }
  
  
  
  public static String getApplicationUri() {
    try {
      FacesContext ctxt = FacesContext.getCurrentInstance();
      ExternalContext ext = ctxt.getExternalContext();
      URI uri = new URI(ext.getRequestScheme(),
              null, ext.getRequestServerName(), ext.getRequestServerPort(),
              ext.getRequestContextPath(), null, null);
      return uri.toASCIIString();
    } catch (URISyntaxException e) {
      throw new FacesException(e);
    }
  }

}
