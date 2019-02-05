/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.api.zeppelin.notebook.repo;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.hdfs.DistributedFsService;
import io.hops.hopsworks.common.hdfs.HdfsUsersController;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Options;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsAction;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.NotebookImportDeserializer;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.notebook.repo.NotebookRepoSettingsInfo;
import org.apache.zeppelin.scheduler.Job;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * Backend for storing Notebooks on hdfs
 */
public class HDFSNotebookRepo implements NotebookRepo {

  private final Logger logger = LoggerFactory.getLogger(HDFSNotebookRepo.class);

  private URI filesystemRoot;
  private final ZeppelinConfiguration conf;
  private String hdfsUser;
  private final Configuration hdfsConf;
  private final DistributedFsService dfsService;
  private final UserGroupInformation superuser;
  private final Pattern psuPattern = Pattern.compile("(\\w*)"
      + HdfsUsersController.USER_NAME_DELIMITER + "\\w*");
  private final Pattern pguPattern = Pattern.compile("(\\w*)" +
      Settings.PROJECT_GENERIC_USER_SUFFIX);

  public HDFSNotebookRepo(ZeppelinConfiguration conf) throws IOException {
    this.conf = conf;
    this.hdfsConf = getHadoopConf();
    superuser = UserGroupInformation.getLoginUser();
    try {
//          ("java:global/hopsworks-common-0.2.0-SNAPSHOT/DistributedFsService");
      String applicationName = InitialContext.doLookup("java:app/AppName");
      String moduleName = InitialContext.doLookup("java:module/ModuleName");
      moduleName = moduleName.replace("api", "common");
      dfsService = InitialContext.doLookup
          ("java:global/" + applicationName + "/" + moduleName + "/DistributedFsService");
    } catch (NamingException ex) {
      throw new IOException(ex);
    }
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
    
    DistributedFileSystemOps dfso = null;
    try {
      dfso = getDfs(superuser);
      String url = filesystemRoot.getPath();
      Path path = new Path(url);
      if (!dfso.getFilesystem().exists(path)) {
        logger.info("Notebook dir does not exist.");
        throw new IOException("Notebook dir does not exist.");
      }
    } finally {
      if (null != dfso) {
        dfso.close();
      }
    }
  }

  private String getNotebookDirPath() {
    return filesystemRoot.getPath();
  }

  private Configuration getHadoopConf() {
    Configuration hdfsConfig;
    //get this from variables table
    String hadoopDir = System.getProperty("HADOOP_HOME");
    if (hadoopDir == null) {
      hadoopDir = "/srv/hadoop";
    }
    String hadoopConfDir = hadoopDir + "/etc/hadoop";

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
    hdfsConfig = new Configuration();
    hdfsConfig.addResource(hadoopPath);
    hdfsConfig.addResource(yarnPath);
    hdfsConfig.addResource(hdfsPath);
    hdfsConfig.set("fs.permissions.umask-mode", "000");
    return hdfsConfig;
  }
  
  private DistributedFileSystemOps getDfs(UserGroupInformation ugi) {
    if (null != ugi) {
      if (ugi.getUserName().equals(superuser.getUserName())) {
        return dfsService.getDfsOps();
      }
      return dfsService.getDfsOps(ugi.getUserName());
    }
    return null;
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

  private Path getRootDir(DistributedFileSystemOps dfs) throws IOException {
    Path rootDir = new Path(getPath("/"));
    if (!dfs.getFilesystem().exists(rootDir)) {
      throw new IOException("Root path does not exists");
    }

    if (!dfs.getFilesystem().isDirectory(rootDir)) {
      throw new IOException("Root path is not a directory");
    }
    return rootDir;
  }

  private Note getNote(Path noteDir, DistributedFileSystemOps dfs) throws
          IOException {
    if (!dfs.getFilesystem().isDirectory(noteDir)) {
      throw new IOException(noteDir.toString() + " is not a directory");
    }

    Path noteJson = new Path(noteDir, "note.json");
    if (!dfs.getFilesystem().exists(noteJson)) {
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

  private NoteInfo getNoteInfo(Path noteDir, DistributedFileSystemOps dfs)
      throws IOException {
    Note note = getNote(noteDir, dfs);
    return new NoteInfo(note);
  }

  private DistributedFileSystemOps getDistributedFs(Path path,
          AuthenticationInfo subject, DistributedFileSystemOps dfs)
          throws IOException {
    DistributedFileSystemOps dfsOp = dfs;
    String owner;
    if (dfs.getFilesystem().exists(path)) {
      owner = dfs.getFileStatus(path).getOwner();
    } else {
      owner = subject.getUser();
    }
    if (subject != null && !owner.equals(subject.getUser())) {
      dfsOp = getDfs(UserGroupInformation.createProxyUser(owner,
              UserGroupInformation.getLoginUser()));
    }
    return dfsOp;
  }

  private DistributedFileSystemOps getUserDfs(AuthenticationInfo subject) throws
          IOException {
    UserGroupInformation ugi;
    if (subject == null || "anonymous".equals(subject.getUser())) {
      ugi = UserGroupInformation.createProxyUser(this.hdfsUser,
              UserGroupInformation.getLoginUser());
    } else {
      ugi = UserGroupInformation.createProxyUser(subject.getUser(),
              UserGroupInformation.getLoginUser());
    }
    return getDfs(ugi);
  }
  
  private void closeDfsClient(DistributedFileSystemOps dfso) {
    dfsService.closeDfsClient(dfso);
  }
  
  @Override
  public List<NoteInfo> list(AuthenticationInfo subject) throws IOException {
    DistributedFileSystemOps udfso = null;
    List<NoteInfo> infos = new LinkedList<>();
    
    try {
      udfso = getUserDfs(subject);
      Path rootDir = getRootDir(udfso);
      FileStatus[] children = udfso.listStatus(rootDir);
      
      for (FileStatus f : children) {
        String fileName = f.getPath().getName();
        if (fileName.startsWith(".")
            || fileName.startsWith("#")
            || fileName.startsWith("~")) {
          // skip hidden, temporary files
          continue;
        }
    
        if (!udfso.getFilesystem().isDirectory(f.getPath())) {
          // currently single note is saved like, [NOTE_ID]/note.json.
          // so it must be a directory
          continue;
        }
  
        NoteInfo info;
        try {
          info = getNoteInfo(f.getPath(), udfso);
          if (info != null) {
            infos.add(info);
          }
        } catch (Exception e) {
          logger.error("Can't read note " + f.getPath().toString(), e);
        }
      }
    } finally {
      closeDfsClient(udfso);
    }
    
    return infos;
  }
  
  @Override
  public Note get(String noteId, AuthenticationInfo subject) throws IOException {
    DistributedFileSystemOps udfso = null;
    Note note = null;
    try {
      udfso = getUserDfs(subject);
      Path rootDir = getRootDir(udfso);
      Path noteDir = new Path(rootDir, noteId);
      note = getNote(noteDir, udfso);
    } finally {
      closeDfsClient(udfso);
    }
    
    return note;
  }

  @Override
  public synchronized void save(Note note, AuthenticationInfo subject) throws
          IOException {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setPrettyPrinting();
    Gson gson = gsonBuilder.create();
    String json = gson.toJson(note);

    DistributedFileSystemOps udfso = null;
    DistributedFileSystemOps dfsOp = null;
    try {
      udfso = getUserDfs(subject);
      Path rootDir = getRootDir(udfso);
  
      Path noteDir = new Path(rootDir, note.getId());
      //returns dfs for the owner of the dir if the dir exists.
      //so we do not change the owner of the notebook.
      dfsOp = getDistributedFs(noteDir, subject, udfso);
  
      FsPermission fsPermission;
      if (!udfso.getFilesystem().exists(noteDir)) {
        fsPermission = new FsPermission(FsAction.ALL, FsAction.READ_EXECUTE,
            FsAction.NONE, false);
        dfsOp.mkdir(noteDir, fsPermission);
      }
      if (!udfso.getFilesystem().isDirectory(noteDir)) {
        throw new IOException(noteDir.toString() + " is not a directory");
      }
  
      Path noteJson = new Path(noteDir, "note.json");
      Path noteJsonTemp = new Path(noteDir, ".note.json");
      // false means not appending. creates file if not exists
      OutputStream out = dfsOp.getFilesystem().create(noteJsonTemp);
      out.write(json.getBytes(conf.getString(
          ZeppelinConfiguration.ConfVars.ZEPPELIN_ENCODING)));
      out.close();
      dfsOp.getFilesystem().rename(noteJsonTemp, noteJson, Options.Rename
          .OVERWRITE);
  
    } finally {
      if (null != udfso) {
        if (null != dfsOp && !udfso.equals(dfsOp)) {
          closeDfsClient(dfsOp);
        }
        closeDfsClient(udfso);
      }
    }
  }

  @Override
  public void remove(String noteId, AuthenticationInfo subject) throws
          IOException {
    DistributedFileSystemOps dfso = null;
    
    try {
      dfso = getDfs(superuser);
      Path rootDir = getRootDir(dfso);
      String hdfsOwner = dfso.getFileStatus(rootDir).getOwner();
      
      Matcher psuMatcher = psuPattern.matcher(hdfsOwner);
      if (psuMatcher.matches()) {
        String extractedPSUProjectname = psuMatcher.group(1);
        Matcher pguMatcher = pguPattern.matcher(subject.getUser());
        if (pguMatcher.matches()) {
          String extractedPGUProjectname = pguMatcher.group(1);
          if (!extractedPSUProjectname.equals(extractedPGUProjectname)) {
            throw new IOException("User <" + subject.getUser() + "> does not " +
                "belong to project");
          }
        }
      }
      
      Path noteDir = new Path(rootDir, noteId);
  
      if (!dfso.getFilesystem().exists(noteDir)) {
        // nothing to do
        return;
      }
  
      if (!dfso.getFilesystem().isDirectory(noteDir)) {
        // it does not look like zeppelin note savings
        throw new IOException("Can not remove " + noteDir.toString());
      }
      dfso.getFilesystem().delete(noteDir, true);
    } finally {
      if (dfso != null) {
        dfsService.closeDfsClient(dfso);
      }
    }
  }

  @Override
  public void close() {
    // no-op
  }

  @Override
  public Revision checkpoint(String noteId, String checkpointMsg,
          AuthenticationInfo subject) throws IOException {
    // no-op
    logger.warn("Checkpoint feature isn't supported in {}", this.getClass().
            toString());
    return Revision.EMPTY;
  }

  @Override
  public List<Revision> revisionHistory(String noteId,
          AuthenticationInfo subject) {
    logger.warn("Get Note revisions feature isn't supported in {}", this.
            getClass().toString());
    return Collections.emptyList();
  }

  @Override
  public Note get(String noteId, String revId, AuthenticationInfo subject)
          throws IOException {
    logger.warn("Get note revision feature isn't supported in {}", this.
            getClass().toString());
    return null;
  }

  @Override
  public Note setNoteRevision(String noteId, String revId,
          AuthenticationInfo subject) throws IOException {
    // Auto-generated method stub
    return null;
  }

  @Override
  public List<NotebookRepoSettingsInfo> getSettings(AuthenticationInfo subject) {
    NotebookRepoSettingsInfo repoSetting = NotebookRepoSettingsInfo.
            newInstance();
    List<NotebookRepoSettingsInfo> settings = Lists.newArrayList();

    repoSetting.name = "Notebook Path";
    repoSetting.type = NotebookRepoSettingsInfo.Type.INPUT;
    repoSetting.value = Collections.emptyList();
    repoSetting.selected = getNotebookDirPath();

    settings.add(repoSetting);
    return settings;
  }

  @Override
  public void updateSettings(Map<String, String> settings,
          AuthenticationInfo subject) {
    if (settings == null || settings.isEmpty()) {
      logger.error("Cannot update {} with empty settings", this.getClass().
              getName());
      return;
    }
    String newNotebookDirectotyPath = StringUtils.EMPTY;
    if (settings.containsKey("Notebook Path")) {
      newNotebookDirectotyPath = settings.get("Notebook Path");
    }

    if (StringUtils.isBlank(newNotebookDirectotyPath)) {
      logger.error("Notebook path is invalid");
      return;
    }
    logger.warn("{} will change notebook dir from {} to {}",
            subject.getUser(), getNotebookDirPath(), newNotebookDirectotyPath);
    try {
      setNotebookDirectory(newNotebookDirectotyPath);
    } catch (IOException e) {
      logger.error("Cannot update notebook directory", e);
    }
  }

}
