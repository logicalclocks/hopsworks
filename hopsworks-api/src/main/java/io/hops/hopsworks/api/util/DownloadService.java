package io.hops.hopsworks.api.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.AccessControlException;
import io.hops.hopsworks.api.filter.AllowedProjectRoles;
import io.hops.hopsworks.api.filter.NoCacheResponse;
import io.hops.hopsworks.api.project.util.DsPath;
import io.hops.hopsworks.api.project.util.PathValidator;
import io.hops.hopsworks.common.constants.message.ResponseMessages;
import io.hops.hopsworks.common.dao.certificates.CertsFacade;
import io.hops.hopsworks.common.dao.dataset.Dataset;
import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.dao.user.security.ua.UserManager;
import io.hops.hopsworks.common.exception.AppException;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.message.MessageController;
import io.hops.hopsworks.common.project.ProjectController;
import io.hops.hopsworks.common.user.CertificateMaterializer;
import io.hops.hopsworks.common.util.EmailBean;
import io.hops.hopsworks.common.util.HopsUtils;
import io.hops.hopsworks.common.util.Settings;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.mail.Message;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.net.util.Base64;

@RequestScoped
@TransactionAttribute(TransactionAttributeType.NEVER)
public class DownloadService {

  private static final Logger LOG = Logger.getLogger(DownloadService.class.getName());

  @EJB
  private DistributedFsService dfs;
  @EJB
  private PathValidator pathValidator;
  @EJB
  private CertificateMaterializer certificateMaterializer;
  @EJB
  private Settings settings;
  @EJB
  private CertsFacade certsFacade;
  @EJB
  private ProjectController projectsController;
  @EJB
  private MessageController messageController;
  @EJB
  private UserManager userBean;
  @EJB
  private EmailBean email;
  @EJB
  private NoCacheResponse noCacheResponse;

  private String projectUsername;
  private Users user;
  private Project project;

  public DownloadService() {
  }

  public void setProjectUsername(String projectUsername) {
    this.projectUsername = projectUsername;
  }

  public void setProject(Project project) {
    this.project = project;
  }

  public void setUser(Users user) {
    this.user = user;
  }

  @GET
  @javax.ws.rs.Path("/{path: .+}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response downloadFromHDFS(@PathParam("path") String path, @Context SecurityContext sc) throws AppException,
      AccessControlException {

    DsPath dsPath = pathValidator.validatePath(this.project, path);
    String fullPath = dsPath.getFullPath().toString();
    Dataset ds = dsPath.getDs();
    if (ds.isShared() && !ds.isEditable() && !ds.isPublicDs()) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          ResponseMessages.DOWNLOAD_ERROR);
    }

    FSDataInputStream stream;
    DistributedFileSystemOps udfso;
    try {
      if (projectUsername != null) {
        udfso = dfs.getDfsOps(projectUsername);
        stream = udfso.open(new Path(fullPath));
        Response.ResponseBuilder response = Response.ok(buildOutputStream(stream, udfso));
        response.header("Content-disposition", "attachment;");
        return response.build();
      } else {
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
            ResponseMessages.DOWNLOAD_ERROR);
      }

    } catch (AccessControlException ex) {
      throw new AccessControlException(
          "Permission denied: You can not download the file ");
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "File does not exist: " + fullPath);
    }
  }

  @GET
  @javax.ws.rs.Path("/certs/{path: .+}")
  @Produces(MediaType.APPLICATION_OCTET_STREAM)
  @AllowedProjectRoles({AllowedProjectRoles.DATA_SCIENTIST, AllowedProjectRoles.DATA_OWNER})
  public Response downloadCerts(
      @PathParam("path") String path,
      @QueryParam("password") String password,
      @Context SecurityContext sc) throws AppException,
      AccessControlException {
    if (user.getEmail().equals(Settings.SITE_EMAIL)
        || user.getEmail().equals(Settings.AGENT_EMAIL)
        || !user.getPassword().equals(DigestUtils.sha256Hex(password))) {
      return noCacheResponse.getNoCacheResponseBuilder(Response.Status.FORBIDDEN).build();
    }
    String zipName = settings.getHopsworksTmpCertDir() + File.separator + project.getName() + "-" + user.getUsername()
        + "-certs.zip";
    java.nio.file.Path zipPath = null;
    try {
      //Read certs from database and stream them out
      certificateMaterializer.materializeCertificates(user.getUsername(), project.getName());
      String kStore = settings.getHopsworksTmpCertDir() + File.separator + HopsUtils.getProjectKeystoreName(project.
          getName(), user.getUsername());
      String tStore = settings.getHopsworksTmpCertDir() + File.separator + HopsUtils.
          getProjectTruststoreName(project.getName(), user.getUsername());

      //Create zip file with the two certificates and stream it out
      zipFiles(zipName, kStore, tStore);
      zipPath = FileSystems.getDefault().getPath(zipName);
      Set<PosixFilePermission> perms = new HashSet<>();
      //add owners permission
      perms.add(PosixFilePermission.OWNER_READ);
      perms.add(PosixFilePermission.OWNER_WRITE);
      Files.setPosixFilePermissions(zipPath, perms);
      try {
        FileInputStream stream = new FileInputStream(zipName);
        Response.ResponseBuilder response = Response.ok(buildOutputStream(stream));
        response.header("Content-disposition", "attachment;");
        //Send email with decrypted password to user
        String certPwd = projectsController.getProjectSpecificCertPw(user, project.getName(),
            Base64.encodeBase64String(certsFacade.findUserCert(project.getName(), user.getUsername()).getUserKey()))
            .getKeyPw();
        //Pop-up a message from admin
        messageController.send(user, userBean.findByEmail(Settings.SITE_EMAIL), "Certificate Info", "",
            "An email was sent with the password for your project's certificates. If an email does not arrive shortly, "
            + "please check spam first and then contact the HopsWorks administrator.", "");
        email.sendEmail(user.getEmail(), Message.RecipientType.TO, "Hopsworks certificate information",
            "The password for keystore and truststore is:" + certPwd);
        return response.build();
      } catch (IOException ex) {
        LOG.log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), ResponseMessages.DOWNLOAD_ERROR);
      } catch (Exception ex) {
        LOG.log(Level.SEVERE, null, ex);
        throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), ResponseMessages.DOWNLOAD_ERROR);
      }
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, null, ex);
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(),
          "Could not retrieve certificates of user: " + projectUsername + ", for project:" + project.getName());
    } finally {
      certificateMaterializer.removeCertificate(projectUsername, project.getName());
      //Remove zipped file
      if (zipPath != null) {
        try {
          Files.deleteIfExists(zipPath);
        } catch (IOException ex) {
          LOG.log(Level.SEVERE, null, ex);
          throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), ResponseMessages.DOWNLOAD_ERROR);
        }
      }
    }

  }

  /**
   *
   * @param zipName
   * @param filePaths
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void zipFiles(String zipName, String... filePaths) throws FileNotFoundException, IOException {
    FileOutputStream fos = new FileOutputStream(zipName);
    try (ZipOutputStream zos = new ZipOutputStream(fos)) {
      for (String aFile : filePaths) {
        zos.putNextEntry(new ZipEntry(new File(aFile).getName()));
        byte[] bytes = Files.readAllBytes(Paths.get(aFile));
        zos.write(bytes, 0, bytes.length);
        zos.closeEntry();
      }
    }
  }

  /**
   *
   * @param stream
   * @param udfso
   * @return
   */
  private StreamingOutput buildOutputStream(final FSDataInputStream stream,
      final DistributedFileSystemOps udfso) {
    StreamingOutput output = new StreamingOutput() {
      @Override
      public void write(OutputStream out) throws IOException,
          WebApplicationException {
        try {
          int length;
          byte[] buffer = new byte[1024];
          while ((length = stream.read(buffer)) != -1) {
            out.write(buffer, 0, length);
          }
          out.flush();
          stream.close();
        } finally {
          dfs.closeDfsClient(udfso);
        }
      }
    };

    return output;
  }

  /**
   *
   * @param stream
   * @return
   */
  private StreamingOutput buildOutputStream(final FileInputStream stream) {
    StreamingOutput output = new StreamingOutput() {
      @Override
      public void write(OutputStream out) throws IOException,
          WebApplicationException {
        int length;
        byte[] buffer = new byte[1024];
        while ((length = stream.read(buffer)) != -1) {
          out.write(buffer, 0, length);
        }
        out.flush();
        stream.close();

      }
    };
    return output;
  }
}
