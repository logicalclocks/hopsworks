package se.kth.hopsworks.rest;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import se.kth.bbc.lims.Constants;
import se.kth.hopsworks.filters.AllowedRoles;

/**
 *
 * @author ermiasg
 */
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DownloadService {
  private static final Logger LOG
          = Logger.getLogger(DownloadService.class.getName());

  private String path;

  public DownloadService() {
  }
  
  public void setPath(String path) {
    this.path = path;
  }
  
  @GET
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response downloadFromHDFS() throws AppException {
    Configuration conf = new Configuration();
    conf.addResource(new Path(Constants.HADOOP_CONF_DIR + "core-site.xml"));
    FileSystem hdfs;
    FSDataInputStream stream;
    try {
      hdfs = FileSystem.get(conf);
      stream = hdfs.open(new Path(this.path));
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
                "File does not exist: " + this.path);
    }

    Response.ResponseBuilder response = Response.ok((Object)stream);
    response.header("Content-disposition", "attachment;");

    return response.build();
  }

}
