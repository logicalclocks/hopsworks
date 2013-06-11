package se.kth.kthfsdashboard.rest.resources;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import se.kth.kthfsdashboard.util.CollectdTools;

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
           @QueryParam("hostname") String hostname,
           @QueryParam("plugin") String plugin,
           @QueryParam("plugin_instance") String pluginInstance,
           @QueryParam("type") String type,
           @QueryParam("type_instance") String typeInstance,
           @QueryParam("ds") String ds,
           @QueryParam("n") int n) throws InterruptedException, IOException {

      CollectdTools collectdTools = new CollectdTools();
      try {
         ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
         InputStream inputStream = collectdTools.getGraphStream(chartType, hostname, plugin, pluginInstance, type, typeInstance, ds, start, end, n);
         BufferedImage image = ImageIO.read(inputStream);
         ImageIO.write(image, "png", byteArrayOutputStream);
         byte[] imageData = byteArrayOutputStream.toByteArray();
         return Response.ok(new ByteArrayInputStream(imageData)).build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "Image == null");
         return Response.status(Response.Status.NOT_FOUND).build();
      }
   }
}
