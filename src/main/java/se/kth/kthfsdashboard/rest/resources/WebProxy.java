package se.kth.kthfsdashboard.rest.resources;

import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
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

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Path("/web")
@Stateless
@RolesAllowed({"ADMIN"})
public class WebProxy {

    final static Logger logger = Logger.getLogger(WebProxy.class.getName());

//  TODO Change MediaType to TEXT_PLAIN if content is text (e.g. log files)
    @GET
    @Path("get/{ip}/{port}/{path:.*}")
    @Produces(MediaType.TEXT_HTML)
    public Response getWebPage(@Context UriInfo uriInfo,
            @PathParam("ip") String ip,
            @PathParam("port") String port,
            @PathParam("path") String path) throws UnsupportedEncodingException {

        WebCommunication web = new WebCommunication();
        String url = "http://" + ip + ":" + port + "/" + path;
        String html = web.getWebPage(url);
        String target = uriInfo.getBaseUri().getPath() + "web/get/" + ip + "/" + port + "/";
        html = html.replaceAll("href=\"/", "href=\"" + target);
        html = html.replaceAll("HREF=\"/", "href=\"" + target);        
        return Response.ok(html).build();
    }
}
