package se.kth.kthfsdashboard.rest.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import se.kth.kthfsdashboard.utils.CollectdUtils;
import se.kth.kthfsdashboard.utils.GraphicsUtils;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Path("/collectd")
@Stateless
@RolesAllowed({"AGENT", "ADMIN"})
public class CollectdResource {

    final static Logger logger = Logger.getLogger(CollectdResource.class.getName());

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "KTHFS Dashboard: Pong";
    }

    @GET
    @Path("graph")
    @Produces("image/png")
    public Response getGraph(
            @QueryParam("chart_type") String chartType,
            @QueryParam("start") int start,
            @QueryParam("end") int end,
            @QueryParam("host") String host,
            @QueryParam("plugin") String plugin,
            @QueryParam("plugin_instance") String pluginInstance,
            @QueryParam("type") String type,
            @QueryParam("type_instance") String typeInstance,
            @QueryParam("ds") String ds,
            @QueryParam("n") int n) throws InterruptedException, IOException {

        byte[] imageByteArray;
        try {
            InputStream inputStream = CollectdUtils.getGraphStream(chartType, host, plugin, pluginInstance, type, typeInstance, ds, start, end, n);
            imageByteArray = GraphicsUtils.convertImageInputStreamToByteArray(inputStream);
        } catch (Exception e) {
            String msg = String.format("Graph not available. Check RRD files for %s: %s / %s @ %s ", chartType, plugin, type, host);
            logger.log(Level.SEVERE, "CollectdResource: Image == null :{0}", msg);
            imageByteArray = GraphicsUtils.errorImage(msg);
        }
        return Response.ok(new ByteArrayInputStream(imageByteArray)).build();
    }
}
