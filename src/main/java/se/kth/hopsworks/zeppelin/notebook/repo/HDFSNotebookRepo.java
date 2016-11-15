package se.kth.hopsworks.zeppelin.notebook.repo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PrivilegedExceptionAction;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Options;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.NotebookImportDeserializer;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.scheduler.Job;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Backend for storing Notebooks on hdfs
 */
public class HDFSNotebookRepo implements NotebookRepo {

  private final Logger logger = LoggerFactory.getLogger(HDFSNotebookRepo.class);

  private DistributedFileSystem dfs;
  private URI filesystemRoot;
  private final ZeppelinConfiguration conf;
  private String hdfsUser;

  public HDFSNotebookRepo(ZeppelinConfiguration conf) throws IOException {
    this.conf = conf;
    setNotebookDirectory(this.conf.getNotebookDir());
  }

  private void setNotebookDirectory(String notebookDir) throws IOException {
    try {
      filesystemRoot = new URI(notebookDir);
    } catch (URISyntaxException e1) {
      throw new IOException(e1);
    }
    UserGroupInformation ugi = UserGroupInformation.getCurrentUser();
    this.hdfsUser = ugi.getShortUserName();
    dfs = getDfs(ugi);
    Path path = new Path(filesystemRoot.getPath());
    if (!dfs.exists(path)) {
      logger.info("Notebook dir doesn't exist, create.");
      FsPermission fsPermission = new FsPermission(FsAction.ALL, FsAction.ALL,
              FsAction.READ_EXECUTE, true);
      dfs.mkdirs(path, fsPermission);
    }
  }

  private Configuration getHadoopConf() {
    Configuration hdfsConf;
    //get this from variables table
    String hadoopConfDir = "/srv/hadoop/etc/hadoop";

    File hdfsConfFile = new File(hadoopConfDir, "hdfs-site.xml");
    if (!hdfsConfFile.exists()) {
      logger.error("Unable to locate configuration file in {0}",
              hdfsConfFile);
      throw new IllegalStateException("No hdfs conf file: hdfs-site.xml");
    }

    File hadoopConfFile = new File(hadoopConfDir, "core-site.xml");
    if (!hadoopConfFile.exists()) {
      logger.error("Unable to locate configuration file in {0}",
              hadoopConfFile);
      throw new IllegalStateException("No hadoop conf file: core-site.xml");
    }

    File yarnConfFile = new File(hadoopConfDir, "yarn-site.xml");
    if (!yarnConfFile.exists()) {
      logger.error("Unable to locate configuration file in {0}",
              yarnConfFile);
      throw new IllegalStateException("No yarn conf file: yarn-site.xml");
    }

    //Set the Configuration object for the hdfs client
    Path yarnPath = new Path(yarnConfFile.getAbsolutePath());
    Path hdfsPath = new Path(hdfsConfFile.getAbsolutePath());
    Path hadoopPath = new Path(hadoopConfFile.getAbsolutePath());
    hdfsConf = new Configuration();
    hdfsConf.addResource(hadoopPath);
    hdfsConf.addResource(yarnPath);
    hdfsConf.addResource(hdfsPath);
    hdfsConf.set("fs.permissions.umask-mode", "000");
    return hdfsConf;
  }

  private DistributedFileSystem getDfs(UserGroupInformation ugi) {
    FileSystem fs = null;
    Configuration hdfsConf = getHadoopConf();
    try {
      fs = ugi.doAs((PrivilegedExceptionAction<FileSystem>) ()
              -> FileSystem.get(FileSystem.getDefaultUri(hdfsConf), hdfsConf));
    } catch (IOException | InterruptedException ex) {
      logger.error("Unable to initialize FileSystem", ex);
    }
    return (DistributedFileSystem) fs;
  }

  private String getPath(String path) {
    if (path == null || path.trim().length() == 0) {
      return filesystemRoot.toString();
    }
    if (path.startsWith("/")) {
      return filesystemRoot.toString() + path;
    } else {
      return filesystemRoot.toString() + "/" + path;
    }
  }

  protected Path getRootDir() throws IOException {
    Path rootDir = new Path(getPath("/"));

    if (!dfs.exists(rootDir)) {
      throw new IOException("Root path does not exists");
    }

    if (!dfs.isDirectory(rootDir)) {
      throw new IOException("Root path is not a directory");
    }

    return rootDir;
  }

  private Note getNote(Path noteDir) throws IOException {
    if (!dfs.isDirectory(noteDir)) {
      throw new IOException(noteDir.toString() + " is not a directory");
    }

    Path noteJson = new Path(noteDir, "note.json");
    if (!dfs.exists(noteJson)) {
      throw new IOException(noteJson.toString() + " not found");
    }

    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setPrettyPrinting();
    Gson gson = gsonBuilder.registerTypeAdapter(Date.class,
            new NotebookImportDeserializer())
            .create();

    InputStream ins = dfs.open(noteJson);
    String json = IOUtils.toString(ins, conf.getString(
            ZeppelinConfiguration.ConfVars.ZEPPELIN_ENCODING));
    ins.close();

    Note note = gson.fromJson(json, Note.class);

    for (Paragraph p : note.getParagraphs()) {
      if (p.getStatus() == Job.Status.PENDING || p.getStatus()
              == Job.Status.RUNNING) {
        p.setStatus(Job.Status.ABORT);
      }
    }

    return note;
  }

  private NoteInfo getNoteInfo(Path noteDir) throws IOException {
    Note note = getNote(noteDir);
    return new NoteInfo(note);
  }

  private DistributedFileSystem getDistributedFs(Path path,
          AuthenticationInfo subject, boolean create)
          throws IOException {
    DistributedFileSystem dfsOp = dfs;
    String owner;
    if (create && dfs.exists(path)) {
      owner = dfs.getFileStatus(path).getOwner();
    } else {
      owner = subject.getUser();
    }
    if (!owner.equals(this.hdfsUser)) {
      dfsOp = getDfs(UserGroupInformation.createProxyUser(owner,
              UserGroupInformation.getLoginUser()));
    }
    return dfsOp;
  }

  @Override
  public List<NoteInfo> list(AuthenticationInfo subject) throws IOException {
    Path rootDir = getRootDir();

    FileStatus[] children = dfs.listStatus(rootDir);

    List<NoteInfo> infos = new LinkedList<>();
    for (FileStatus f : children) {
      String fileName = f.getPath().getName();
      if (fileName.startsWith(".")
              || fileName.startsWith("#")
              || fileName.startsWith("~")) {
        // skip hidden, temporary files
        continue;
      }

      if (!dfs.isDirectory(f.getPath())) {
        // currently single note is saved like, [NOTE_ID]/note.json.
        // so it must be a directory
        continue;
      }

      NoteInfo info;

      try {
        info = getNoteInfo(f.getPath());
        if (info != null) {
          infos.add(info);
        }
      } catch (Exception e) {
        logger.error("Can't read note " + f.getPath().toString(), e);
      }
    }

    return infos;
  }

  @Override
  public Note get(String noteId, AuthenticationInfo subject) throws IOException {
    Path rootDir = getRootDir();
    Path noteDir = new Path(rootDir, noteId);

    return getNote(noteDir);
  }

  @Override
  public synchronized void save(Note note, AuthenticationInfo subject) throws
          IOException {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setPrettyPrinting();
    Gson gson = gsonBuilder.create();
    String json = gson.toJson(note);

    Path rootDir = getRootDir();

    Path noteDir = new Path(rootDir, note.id());
    DistributedFileSystem dfsOp = getDistributedFs(noteDir, subject, true);

    FsPermission fsPermission;
    if (!dfs.exists(noteDir)) {
      fsPermission = new FsPermission(FsAction.ALL, FsAction.READ_EXECUTE,
              FsAction.NONE, false);
      dfsOp.mkdir(noteDir, fsPermission);
    }
    if (!dfs.isDirectory(noteDir)) {
      throw new IOException(noteDir.toString() + " is not a directory");
    }

    Path noteJson = new Path(noteDir, "note.json");
    Path noteJsonTemp = new Path(noteDir, ".note.json");
    // false means not appending. creates file if not exists
    OutputStream out = dfsOp.create(noteJsonTemp);
    out.write(json.getBytes(conf.getString(
            ZeppelinConfiguration.ConfVars.ZEPPELIN_ENCODING)));
    out.close();
    dfsOp.rename(noteJsonTemp, noteJson, Options.Rename.OVERWRITE);

    if (!dfs.equals(dfsOp)) {
      dfsOp.close();
    }
  }

  @Override
  public void remove(String noteId, AuthenticationInfo subject) throws
          IOException {
    Path rootDir = getRootDir();
    Path noteDir = new Path(rootDir, noteId);

    if (!dfs.exists(noteDir)) {
      // nothing to do
      return;
    }

    if (!dfs.isDirectory(noteDir)) {
      // it does not look like zeppelin note savings
      throw new IOException("Can not remove " + noteDir.toString());
    }
    DistributedFileSystem dfsOp = getDistributedFs(noteDir, subject, false);
    dfsOp.delete(noteDir, true);

    if (!dfs.equals(dfsOp)) {
      dfsOp.close();
    }
  }

  @Override
  public void close() {
    try {
      dfs.close();
    } catch (IOException ex) {
      logger.info("Could not close dfs object.", ex);
    }
  }

  @Override
  public Revision checkpoint(String noteId, String checkpointMsg,
          AuthenticationInfo subject) throws IOException {
    // Auto-generated method stub
    return null;
  }

  @Override
  public Note get(String noteId, Revision rev, AuthenticationInfo subject)
          throws IOException {
    // Auto-generated method stub
    return null;
  }

  @Override
  public List<Revision> revisionHistory(String noteId,
          AuthenticationInfo subject) {
    // Auto-generated method stub
    return null;
  }

}
