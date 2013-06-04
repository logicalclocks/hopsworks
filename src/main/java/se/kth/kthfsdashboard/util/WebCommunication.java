package se.kth.kthfsdashboard.util;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import se.kth.kthfsdashboard.struct.NodesTableItem;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class WebCommunication {

   public enum Type {

      STDOUT, STDERR, DO
   }
   private static String USERNAME = "kthfsagent@sics.se";
   private static String PASSWORD = "kthfsagent";
   private static String PROTOCOL = "https";
   private static int PORT = 8090;
   private static String NOT_AVAILABLE = "Not available.";
//   private static int LOG_LINES = 50;   
   private static final Logger logger = Logger.getLogger(WebCommunication.class.getName());

   public WebCommunication() {
   }

   private String createUrl(String context, String hostname, String... args) {
      String template = "%s://%s:%s/%s";
      String url = String.format(template, PROTOCOL, hostname, PORT, context);
      for (int i = 0; i < args.length; i++) {
         url += "/" + args[i];
      }
      return url;
   }

   private String fetchContent(String url) {
      String content = NOT_AVAILABLE;
      try {
         ClientResponse response = getWebResource(url);
         if (response.getClientResponseStatus().getFamily() == Response.Status.Family.SUCCESSFUL) {
            content = response.getEntity(String.class);
         }
      } catch (Exception e) {
         logger.log(Level.SEVERE, null, e);
      }
      return content;
   }

   private String fetchLog(String url) {
      String log = fetchContent(url);
      log = log.replaceAll("\n", "<br>");
      return log;
   }

   public String getConfig(String hostname, String cluster, String service, String role) {
      String url = createUrl("config", hostname, cluster, service, role);
      return fetchContent(url);
   }

   public String getRoleLog(String hostname, String cluster, String service, String role, int lines) {
      String url = createUrl("log", hostname, cluster, service, role, String.valueOf(lines));
      return fetchLog(url);
   }

   public String getServiceLog(String hostname, String cluster, String service, int lines) {
      String url = createUrl("log", hostname, cluster, service, String.valueOf(lines));
      return fetchLog(url);
   }

   public String getAgentLog(String hostname, int lines) {
      String url = createUrl("agentlog", hostname, String.valueOf(lines));
      return fetchLog(url);
   }

   public List<NodesTableItem> getNdbinfoNodesTable(String hostname) {
      List<NodesTableItem> resultList = new ArrayList<NodesTableItem>();

      String url = createUrl("mysql", hostname, "ndbinfo", "nodes");
      String jsonString = fetchContent(url);
      try {
         JSONArray json = new JSONArray(jsonString);
         if (json.get(0).equals( "Error")) {
            resultList.add(new NodesTableItem(null, json.getString(1), null, null, null));
            return resultList;
         }
         for (int i = 0; i < json.length(); i++) {
            JSONArray node = json.getJSONArray(i);
            Integer nodeId = node.getInt(0);
            Long uptime = node.getLong(1);            
            String status = node.getString(2);
            Integer startPhase = node.getInt(3);
            Integer configGeneration = node.getInt(4);
            resultList.add(new NodesTableItem(nodeId, status, uptime, startPhase, configGeneration));
         }
      } catch (Exception ex) {
         logger.log(Level.SEVERE, "Exception: {0}", ex);
         resultList.add(new NodesTableItem(null, "Error", null, null, null));         
      }
      return resultList;
   }

   public ClientResponse doCommand(String hostname, String cluster, String service, String role, String command) throws Exception {
      String url = createUrl("do", hostname, cluster, service, role, command);
      return getWebResource(url);
   }

   private ClientResponse getWebResource(String url) throws Exception {

      disableCertificateValidation();
      Client client = Client.create();
      WebResource webResource = client.resource(url);
      MultivaluedMap params = new MultivaluedMapImpl();
      params.add("username", USERNAME);
      params.add("password", PASSWORD);
      return webResource.queryParams(params).get(ClientResponse.class);
   }

   private static void disableCertificateValidation() {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{
         new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
               return new X509Certificate[0];
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
         }};

      // Ignore differences between given hostname and certificate hostname
      HostnameVerifier hv = new HostnameVerifier() {
         public boolean verify(String hostname, SSLSession session) {
            return true;
         }
      };

      // Install the all-trusting trust manager
      try {
         SSLContext sc = SSLContext.getInstance("SSL");
         sc.init(null, trustAllCerts, new SecureRandom());
         HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
         HttpsURLConnection.setDefaultHostnameVerifier(hv);
      } catch (Exception e) {
      }
   }
}