package io.hops.kibana;

import static io.hops.kibana.ProxyServlet.ATTR_TARGET_URI;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.AbortableHttpRequest;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import se.kth.bbc.project.ProjectTeam;
import se.kth.bbc.security.ua.UserManager;
import se.kth.hopsworks.controller.ProjectController;
import se.kth.hopsworks.user.model.Users;

/**
 *
 * Authorizes HopsWorks users to access particular elasticsearch indices
 * to be displayed by Kibana.
 * <p>
 */
@Stateless
public class KibanaProxyServlet extends ProxyServlet {

  @EJB
  private UserManager userManager;
  @EJB
  private ProjectController projectController;

  /**
   * Authorizer user to access particular index.
   * 
   * @param servletRequest
   * @param servletResponse
   * @throws ServletException
   * @throws IOException 
   */
  @Override
  protected void service(HttpServletRequest servletRequest,
          HttpServletResponse servletResponse) throws ServletException,
          IOException {
    String userEmail = servletRequest.getUserPrincipal().getName();
    System.out.println("proxy :: servletRequest.userEmail:" + userEmail);
    Users user = userManager.getUserByEmail(userEmail);
    System.out.println("proxy :: servletRequest.user.lastname:" + user.
            getLname());

    System.out.println("proxy :: servletRequest.getRequestURI():"
            + servletRequest.getRequestURI());
    boolean found = false;
    
    //Filter requests based on path
    /**
     * List of requests
     * 1.  http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/_mget?timeout=0&ignore_unavailable=true&preference=1484560870227
     * 2.  http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/index-pattern/_search?fields= (filter json response)
     * 3. "http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/_mapping/* /field/_source?_=1484560870948 (do nothing)
     * 4.  http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/_mget?timeout=0&ignore_unavailable=true&preference=1484560870227 (filter request params)
     * 5.  http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/_msearch?timeout=0&ignore_unavailable=true&preference=1484560870227 (filter request payload)
     * 6.  http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/index-pattern/demo_admin000 (filter uri)
     * 7.  http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/logstash-* /_mapping/field/*?_=1484561606214&ignore_unavailable=false&allow_no_indices=false&include_defaults=true (filter index in URI)
     * 8.  http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/search/_search?size=100 (filter saved searches in response, should be prefixed with projectId)
     * 9.  http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/visualization/_search?size=100 (similar 8)
     * 10. http://tkak2.sics.se:8080/hopsworks/kibana/elasticsearch/.kibana/dashboard/_search?size=100 (similar to 8)
     * 
     */
    //2
    if(servletRequest.getRequestURI().contains("elasticsearch/.kibana/index-pattern/_search")){
      
    }//6
    else if (servletRequest.getRequestURI().contains(
            "elasticsearch/.kibana/index-pattern")) {
      //Get index from URI
      String index = servletRequest.getRequestURI().substring(servletRequest.
              getRequestURI().lastIndexOf("/")).replace("/", "");
      //Check if this user has access to this project
      List<ProjectTeam> projectTeams = projectController.findProjectByUser(
              userEmail);

      for (ProjectTeam team : projectTeams) {
        if (team.getProject().getName().equalsIgnoreCase(index)) {
          found = true;
          break;
        }
      }
      System.out.println("proxy :: servletRequest.getPathTranslated():"
              + servletRequest.getPathTranslated());
      System.out.println("proxy :: servletRequest.getServletPath():"
              + servletRequest.getServletPath());
      System.out.println("proxy :: servletRequest.getPathInfo:"
              + servletRequest.getPathInfo());
      System.out.println("proxy :: servletRequest.getQueryString():"
              + servletRequest.getQueryString());
      if (!found) {
        return;
      }
    } // 
    else if (servletRequest.getRequestURI().contains("elasticsearch/.kibana/index-pattern/_search")){
      
    }

    //initialize request attributes from caches if unset by a subclass by this point
    if (servletRequest.getAttribute(ATTR_TARGET_URI) == null) {
      servletRequest.setAttribute(ATTR_TARGET_URI, targetUri);
    }
    if (servletRequest.getAttribute(ATTR_TARGET_HOST) == null) {
      servletRequest.setAttribute(ATTR_TARGET_HOST, targetHost);
    }

    // Make the Request
    //note: we won't transfer the protocol version because I'm not sure it would truly be compatible
    String method = servletRequest.getMethod();
    String proxyRequestUri = rewriteUrlFromRequest(servletRequest);
    HttpRequest proxyRequest;
    //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
    if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null
            || servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
      HttpEntityEnclosingRequest eProxyRequest
              = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
      // Add the input entity (streamed)
      //  note: we don't bother ensuring we close the servletInputStream since the container handles it
      eProxyRequest.setEntity(new InputStreamEntity(servletRequest.
              getInputStream(), servletRequest.getContentLength()));
      proxyRequest = eProxyRequest;
    } else {
      proxyRequest = new BasicHttpRequest(method, proxyRequestUri);
    }

    copyRequestHeaders(servletRequest, proxyRequest);

    super.setXForwardedForHeader(servletRequest, proxyRequest);

    HttpResponse proxyResponse = null;
    try {
      // Execute the request
      if (doLog) {
        log("proxy " + method + " uri: " + servletRequest.getRequestURI()
                + " -- " + proxyRequest.getRequestLine().getUri());
      }
      proxyResponse = super.proxyClient.execute(super.getTargetHost(
              servletRequest), proxyRequest);

      // Process the response
      int statusCode = proxyResponse.getStatusLine().getStatusCode();

      if (doResponseRedirectOrNotModifiedLogic(servletRequest, servletResponse,
              proxyResponse, statusCode)) {
        //the response is already "committed" now without any body to send
        //TODO copy response headers?
        return;
      }

      // Pass the response code. This method with the "reason phrase" is deprecated but it's the only way to pass the
      //  reason along too.
      //noinspection deprecation
      servletResponse.setStatus(statusCode, proxyResponse.getStatusLine().
              getReasonPhrase());

      copyResponseHeaders(proxyResponse, servletRequest, servletResponse);

      // Send the content to the client
      copyResponseEntity(proxyResponse, servletResponse);

    } catch (Exception e) {
      //abort request, according to best practice with HttpClient
      if (proxyRequest instanceof AbortableHttpRequest) {
        AbortableHttpRequest abortableHttpRequest
                = (AbortableHttpRequest) proxyRequest;
        abortableHttpRequest.abort();
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      if (e instanceof ServletException) {
        throw (ServletException) e;
      }
      //noinspection ConstantConditions
      if (e instanceof IOException) {
        throw (IOException) e;
      }
      throw new RuntimeException(e);

    } finally {
      // make sure the entire entity was consumed, so the connection is released
      if (proxyResponse != null) {
        consumeQuietly(proxyResponse.getEntity());
      }
      //Note: Don't need to close servlet outputStream:
      // http://stackoverflow.com/questions/1159168/should-one-call-close-on-httpservletresponse-getoutputstream-getwriter
    }
  }
}
