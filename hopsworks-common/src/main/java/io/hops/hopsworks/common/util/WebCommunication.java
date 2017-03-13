package io.hops.hopsworks.common.util;

import io.hops.hopsworks.common.dao.pythonDeps.PythonDepsFacade;
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
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status.Family;
import org.apache.commons.lang.StringEscapeUtils;

@Stateless
public class WebCommunication {

  private static final Logger logger = Logger.getLogger(WebCommunication.class.
          getName());

  private static boolean DISABLE_CERTIFICATE_VALIDATION = true;
  private static String PROTOCOL = "https";
  private static int PORT = 8090;
  private static String NOT_AVAILABLE = "Not available.";
  @EJB
  private Settings settings;

  public WebCommunication() {
  }

  public Response getWebResponse(String url, String agentPassword) {
    Response response;
    try {
      response = getWebResource(url, agentPassword);
      return response;
    } catch (Exception ex) {
      logger.log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   *
   * @param operation start | stop | restart
   * @param hostAddress
   * @param agentPassword
   * @param cluster
   * @param service
   * @param role
   * @return
   */
  public String roleOp(String operation, String hostAddress,
          String agentPassword, String cluster, String service, String role) {
    String url = createUrl(operation, hostAddress, cluster, service, role);
    return fetchContent(url, agentPassword);
  }

  public String getConfig(String hostAddress, String agentPassword,
          String cluster, String service, String role) {
    String url = createUrl("config", hostAddress, cluster, service, role);
    return fetchContent(url, agentPassword);
  }

  public String getRoleLog(String hostAddress, String agentPassword,
          String cluster, String service, String role, int lines) {
    String url = createUrl("log", hostAddress, cluster, service, role, String.
            valueOf(lines));
    return fetchLog(url, agentPassword);
  }

  public String getServiceLog(String hostAddress, String agentPassword,
          String cluster, String service, int lines) {
    String url = createUrl("log", hostAddress, cluster, service, String.valueOf(
            lines));
    return fetchLog(url, agentPassword);
  }

  public String getAgentLog(String hostAddress, String agentPassword, int lines) {
    String url = createUrl("agentlog", hostAddress, String.valueOf(lines));
    return fetchLog(url, agentPassword);
  }

  public List<NodesTableItem> getNdbinfoNodesTable(String hostAddress,
          String agentPassword) {
    List<NodesTableItem> resultList = new ArrayList<NodesTableItem>();

    String url = createUrl("mysql", hostAddress, "ndbinfo", "nodes");
    String jsonString = fetchContent(url, agentPassword);
    InputStream stream = new ByteArrayInputStream(jsonString.getBytes(
            StandardCharsets.UTF_8));
    try {
      JsonArray json = Json.createReader(stream).readArray();
      if (json.get(0).equals("Error")) {
        resultList.add(new NodesTableItem(null, json.getString(1), null, null,
                null));
        return resultList;
      }
      for (int i = 0; i < json.size(); i++) {
        JsonArray node = json.getJsonArray(i);
        Integer nodeId = node.getInt(0);
        Long uptime = node.getJsonNumber(1).longValue();
        String status = node.getString(2);
        Integer startPhase = node.getInt(3);
        Integer configGeneration = node.getInt(4);
        resultList.add(new NodesTableItem(nodeId, status, uptime, startPhase,
                configGeneration));
      }
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Exception: {0}", ex);
      resultList.add(new NodesTableItem(null, "Error", null, null, null));
    }
    return resultList;
  }

  public String executeRun(String hostAddress, String agentPassword,
          String cluster, String service, String role, String command,
          String[] params) throws Exception {
    return execute("execute/run", hostAddress, agentPassword, cluster, service,
            role, command, params);
  }

  public String executeStart(String hostAddress, String agentPassword,
          String cluster, String service, String role, String command,
          String[] params) throws Exception {
    return execute("execute/start", hostAddress, agentPassword, cluster, service,
            role, command, params);
  }

  public String executeContinue(String hostAddress, String agentPassword,
          String cluster, String service, String role, String command,
          String[] params) throws Exception {
    return execute("execute/continue", hostAddress, agentPassword, cluster,
            service, role, command, params);
  }

  private String execute(String path, String hostAddress, String agentPassword,
          String cluster, String service, String role, String command,
          String[] params) throws Exception {
    String url = createUrl(path, hostAddress, cluster, service, role, command);
    String optionsAndParams = "";
    for (String param : params) {
      optionsAndParams += optionsAndParams.isEmpty() ? param : " " + param;
    }
    Response response = postWebResource(url, agentPassword,
            optionsAndParams);
    int code = response.getStatus();
    Family res = Response.Status.Family.familyOf(code);
    if (res == Response.Status.Family.SUCCESSFUL) {
      String responseString = response.readEntity(String.class);
      if (path.equalsIgnoreCase("execute/continue")) {
        JsonObject json = Json.createReader(response.readEntity(Reader.class)).
                readObject();
        responseString = json.getString("before");
      }
      return FormatUtils.stdoutToHtml(responseString);
    }
    throw new RuntimeException("Did not succeed to execute command.");
  }

  public Response doCommand(String hostAddress, String agentPassword,
          String cluster, String service, String role, String command) throws
          Exception {
    String url = createUrl("do", hostAddress, agentPassword, cluster, service,
            role, command);
    return getWebResource(url, agentPassword);
  }

  private String createUrl(String context, String hostAddress, String... args) {
    String template = "%s://%s:%s/%s";
    String url = String.format(template, PROTOCOL, hostAddress, PORT, context);
    for (int i = 0; i < args.length; i++) {
      url += "/" + args[i];
    }
    return url;
  }

  private String fetchContent(String url, String agentPassword) {
    String content = NOT_AVAILABLE;
    try {
      Response response = getWebResource(url, agentPassword);
      int code = response.getStatus();
      Family res = Response.Status.Family.familyOf(code);
      if (res == Response.Status.Family.SUCCESSFUL) {
        content = response.readEntity(String.class);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, null, e);
    }
    return content;
  }

  private String fetchLog(String url, String agentPassword) {
    String log = fetchContent(url, agentPassword);
//        log = log.replaceAll("\n", "<br>");
    log = FormatUtils.stdoutToHtml(log);
    return log;
  }

  private Response getWebResource(String url, String agentPassword) throws
          Exception {
    return getWebResource(url, agentPassword, null);
  }

  private Response getWebResource(String url, String agentPassword,
          Map<String, String> args) throws
          Exception {

    if (DISABLE_CERTIFICATE_VALIDATION) {
      disableCertificateValidation();
    }
    Client client = ClientBuilder.newClient();
    WebTarget webResource = client.target(url);

    webResource.queryParam("username", Settings.AGENT_EMAIL);
    webResource.queryParam("password", agentPassword);
    if (args != null) {
      for (String key : args.keySet()) {
        webResource.queryParam(key, args.get(key));
      }
    }
    logger.log(Level.INFO,
            "WebCommunication: Requesting url: {0} with password {1}",
            new Object[]{url, agentPassword});

    Response response = webResource.request()
            .header("Accept-Encoding", "gzip,deflate")
            .get(Response.class);
    logger.log(Level.INFO, "WebCommunication: Requesting url: {0}", url);
    return response;
  }

  private Response postWebResource(String url, String agentPassword,
          String body) throws Exception {
    return postWebResource(url, agentPassword, "", "", body);
  }

  private Response postWebResource(String url, String agentPassword,
          String channelUrl, String version, String body) throws Exception {
    if (DISABLE_CERTIFICATE_VALIDATION) {
      disableCertificateValidation();
    }
    Client client = ClientBuilder.newClient();
    WebTarget webResource = client.target(url);
    webResource.queryParam("username", Settings.AGENT_EMAIL);
    webResource.queryParam("password", agentPassword);

    Response response = webResource.request()
            .header("Accept-Encoding", "gzip,deflate")
            .post(Entity.entity(body, MediaType.TEXT_PLAIN), Response.class);
    return response;
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
      public boolean verify(String hostAddress, SSLSession session) {
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
      // ??
    }
  }

  public int anaconda(String hostAddress, String agentPassword, String op,
          String project, String arg) throws Exception {

    String path = "anaconda/" + settings.getSparkUser() + '/' + op.toLowerCase()
            + "/" + project;
    String template = "%s://%s:%s/%s";
    String url = String.format(template, PROTOCOL, hostAddress, PORT, path);
    Map<String, String> args = null;
    if (op.compareToIgnoreCase(PythonDepsFacade.CondaOp.CLONE.toString())
            == 0) {
      args = new HashMap<>();
      if (arg == null || arg.isEmpty()) {
        throw new RuntimeException(
                "You forgot the 'srcProject' argument for the conda "
                + "clone environment command for project " + project);
      }
      args.put("srcproj", arg);
    }
    Response response = getWebResource(url, agentPassword, args);
    int code = response.getStatus();
    Family res = Response.Status.Family.familyOf(code);
    if (res == Response.Status.Family.SUCCESSFUL) {
      return response.getStatus();
    }
    throw new RuntimeException("Error. Failed to execute anaconda command " + op
            + " on " + project + ". Result was: " + res);
  }

  public int conda(String hostAddress, String agentPassword, String op,
          String project, String channel, String lib, String version) throws
          Exception {

    String template = "%s://%s:%s/%s";
    String channelEscaped = StringEscapeUtils.escapeJava(channel);
    String path = "conda/" + settings.getSparkUser() + '/' + op.toLowerCase()
            + "/" + project + "/" + lib;

    String url = String.format(template, PROTOCOL, hostAddress, PORT, path);

    Map<String, String> args = new HashMap<>();

    if (!channel.isEmpty()) {
      args.put("channelurl", channelEscaped);
    }
    if (!version.isEmpty()) {
      args.put("version", version);
    }

    Response response = getWebResource(url, agentPassword, args);
    int code = response.getStatus();
    Family res = Response.Status.Family.familyOf(code);
    if (res == Response.Status.Family.SUCCESSFUL) {
      return response.getStatus();
    }
    throw new RuntimeException("Error. Failed to execute conda command " + op
            + " on " + project + ". Result was: " + res);
  }

}
