package se.kth.kthfsdashboard.rest.resources;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import se.kth.kthfsdashboard.utils.CollectdUtils;

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

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      try {
         InputStream inputStream = CollectdUtils.getGraphStream(chartType, host, plugin, pluginInstance, type, typeInstance, ds, start, end, n);
         BufferedImage image = ImageIO.read(inputStream);
         ImageIO.write(image, "png", byteArrayOutputStream);
         byte[] imageData = byteArrayOutputStream.toByteArray();
         return Response.ok(new ByteArrayInputStream(imageData)).build();
      } catch (Exception e) {
         logger.log(Level.SEVERE, "CollectdResource >> Image == null : check rrd file for {0}: {1}/{2} @ {3} ", new Object[]{chartType, plugin, type, host});
         String msg = String.format("Graph not available. Check RRD files for %s: %s / %s @ %s ", chartType, plugin, type, host);
         BufferedImage image = createErrorImage(msg);
         ImageIO.write(image, "png", byteArrayOutputStream);
         byte[] imageData = byteArrayOutputStream.toByteArray();
         return Response.ok(new ByteArrayInputStream(imageData)).build();
      }
   }

   private BufferedImage createErrorImage(String msg) {
      int w = 350;
      int h = 200;
      BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = img.createGraphics();
      g2d.setPaint(Color.black);
      g2d.setFont(new Font("Arial", Font.PLAIN, 12));
      FontMetrics fm = g2d.getFontMetrics();
      int x = 5;
      int y = fm.getHeight();
      drawString(g2d, x, y, w, h, msg);
      g2d.dispose();
      return img;
   }

   private void drawString(Graphics2D g2d, int x1, int y1, int x2, int y2, String txt) {
      float interline = 1;
      float width = x2 - x1;
      AttributedString as = new AttributedString(txt);
      as.addAttribute(TextAttribute.FOREGROUND, g2d.getPaint());
      as.addAttribute(TextAttribute.FONT, g2d.getFont());
      AttributedCharacterIterator aci = as.getIterator();
      FontRenderContext frc = new FontRenderContext(null, true, false);
      LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
      while (lbm.getPosition() < txt.length()) {
         TextLayout tl = lbm.nextLayout(width);
         y1 += tl.getAscent();
         tl.draw(g2d, x1, y1);
         y1 += tl.getDescent() + tl.getLeading() + (interline - 1.0f) * tl.getAscent();
         if (y1 > y2) {
            break;
         }
      }
   }
}
