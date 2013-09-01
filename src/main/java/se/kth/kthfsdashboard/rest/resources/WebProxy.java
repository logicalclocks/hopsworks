package se.kth.kthfsdashboard.rest.resources;

import com.sun.jersey.api.client.ClientResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import se.kth.kthfsdashboard.communication.WebCommunication;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.role.RoleEJB;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Path("/web")
@Stateless
@RolesAllowed({"ADMIN"})
public class WebProxy {

    final static Logger logger = Logger.getLogger(WebProxy.class.getName());
    @EJB
    RoleEJB roleEjb;
    @EJB
    HostEJB hostEJB;

    @GET
    @Path("/{ip}/{port}/{path:.*}")
    @Produces({MediaType.TEXT_HTML})
    public Response getWebPage(@Context UriInfo uriInfo,
            @PathParam("ip") String ip,
            @PathParam("port") String port,
            @PathParam("path") String path) throws UnsupportedEncodingException, IOException, Exception {

        WebCommunication web = new WebCommunication();

        String url = "http://" + ip + ":" + port + "/" + path;
        ClientResponse response = web.getWebResponse(url);
        String contentType = response.getHeaders().getFirst("Content-Type");

        if (contentType.startsWith("text/html")) {
            String html = response.getEntity(String.class);
            String target = uriInfo.getBaseUri().getPath() + "web/" + ip + "/" + port + "/";
            html = html.replaceAll("href=\"/", "href=\"" + target);
            html = html.replaceAll("HREF=\"/", "HREF=\"" + target);
            html = html.replaceAll("src=\"/", "src=\"" + target);
            html = html.replaceAll("SRC=\"/", "SRC=\"" + target);

            //Use WebProxy for links to other roles and change hostname to IP address
            int from = 0;
            int to;
            while (from < html.length() && from != -1) {
                from = html.indexOf("href=\"http://", from);
                if (from != -1) {
                    to = html.indexOf("\"", from + 7);
                    URL u = new URL(html.substring(from + 6, to));
                    String hostId = roleEjb.findHostIdByWebPort(u.getPort(), Integer.parseInt(port));
                    if (hostId != null) {
                        Host h = hostEJB.findHostById(hostId);
                        String ipAddress = h.getPrivateIp();
                        if (ipAddress == null) {
                            ipAddress = h.getPublicIp();
                        }
                        target = uriInfo.getBaseUri().getPath() + "web/" + ipAddress + "/" + u.getPort() + "/" + u.getPath();
                        html = html.replace(u.toString(), target);
                    }
                    from = to + 1;
                }
            }
            return Response.ok(html).type(response.getType()).build();

        } else { // Images, CSS, JS: image/png, text.css, ...            
            String contentEncoding = "";
            if (response.getHeaders().containsKey("Content-Encoding")) {
                for (String encoding : response.getHeaders().get("Content-Encoding")) {
                    contentEncoding = contentEncoding.isEmpty() ? encoding : ";" + encoding;
                }
            }
            return Response.ok().entity(response.getEntityInputStream()).type(response.getType())
                    .header("Content-Encoding", contentEncoding).build();
        }
    }
}
