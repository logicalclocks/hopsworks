package se.kth.hopsworks.rest;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import se.kth.bbc.lims.Constants;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.filters.AllowedRoles;
import se.kth.meta.db.MTableFacade;
import se.kth.meta.db.TupleToFileFacade;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.MTable;
import se.kth.meta.entity.MetaData;
import se.kth.meta.entity.MetaDataView;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.TupleToFile;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author vangelis
 */
@Path("/metadata")
@RolesAllowed({"SYS_ADMIN", "BBC_USER"})
@Produces(MediaType.APPLICATION_JSON)
@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class MetadataService {

  private final static Logger logger = Logger.getLogger(MetadataService.class.
          getName());
  @Inject
  private UploadService uploader;
  @EJB
  private NoCacheResponse noCacheResponse;
  @EJB
  private TupleToFileFacade ttf;
  @EJB
  private MTableFacade mtf;

  private String path;

  /**
   * Uploads a template file (.json) to the file system (hopsfs) and persists it
   * to the database as well
   * <p>
   * @param path
   * @return
   * @throws AppException
   */
  @Path("upload/{path: .+}")
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public UploadService upload(
          @PathParam("path") String path) throws AppException {

    if (path == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              ResponseMessages.UPLOAD_PATH_NOT_SPECIFIED);
    }
    this.path = File.separator + Constants.DIR_ROOT + File.separator + path;

    //sanitize the path
    if (!path.endsWith(File.separator)) {
      this.path += File.separator;
    }

    this.uploader.setUploadPath(this.path);

    return this.uploader;
  }

  /**
   * Fetch metadata associated to an inode by table id
   * <p>
   * @param inodeid
   * @param tableid
   * @param sc
   * @param req
   * @return
   * @throws AppException
   */
  @GET
  @Path("fetchmetadata/{inodeid}/{tableid}")
  @Produces(MediaType.APPLICATION_JSON)
  @AllowedRoles(roles = {AllowedRoles.DATA_SCIENTIST, AllowedRoles.DATA_OWNER})
  public Response fetchMetadata(
          @PathParam("inodeid") Integer inodeid,
          @PathParam("tableid") Integer tableid,
          @Context SecurityContext sc,
          @Context HttpServletRequest req) throws AppException {

    if (inodeid == null || tableid == null) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
              "Incomplete request!");
    }

    //metadata associated to a specific table and inode
    List<MetaDataView> metadata = new LinkedList<>();

    try {
      List<TupleToFile> tuples = ttf.getTuplesByInode(inodeid);
      MTable table = mtf.getTable(tableid);

      List<Field> fields = table.getFields();

      //groups metadata per table
      MetaDataView metatopView = new MetaDataView(table.getName());

      for (Field field : fields) {
        //groups metadata per field
        MetaDataView metainnerView = new MetaDataView(field.getName());

        /*
         * Load raw data based on the field id. Keep only data related to
         * a specific inode
         */
        List<RawData> rawList = field.getRawData();

        for (RawData raw : rawList) {
          for (TupleToFile tuple : tuples) {

            //filter out irrelevant metadata
            if (raw.getRawdataPK().getTupleid() == tuple.getId()) {
              List<MetaData> meta = new LinkedList<>(raw.getMetaData());

              for (MetaData mt : meta) {
                //all metadata for an inode a field carries
                metainnerView.getMetadataView().add(new MetaDataView(mt.
                        getMetaDataPK().getId(), mt.getData()));
              }
            }
          }
        }
        metatopView.getMetadataView().add(metainnerView);
      }

      metadata.add(metatopView);
      GenericEntity<List<MetaDataView>> response
              = new GenericEntity<List<MetaDataView>>(metadata) {
              };

      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.OK).
              entity(response).build();

    } catch (DatabaseException ex) {
      logger.log(Level.WARNING, "Trying to fetch metadata.", ex);
      throw new AppException(Response.Status.NO_CONTENT.getStatusCode(),
              "Could not fetch the metadata for the file " + inodeid + ".");
    }
  }
}
