/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.kth.hopsworks.zeppelin.notebook.repo;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.FileType;
import org.apache.commons.vfs2.NameScope;
import org.apache.commons.vfs2.Selectors;
import org.apache.commons.vfs2.VFS;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.conf.ZeppelinConfiguration.ConfVars;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.scheduler.Job.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Arrays;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import se.kth.bbc.project.Project;

/**
 *
 */
public class FSNotebookRepo implements NotebookRepo {

  Logger logger = LoggerFactory.getLogger(FSNotebookRepo.class);

  private FileSystemManager fsManager;
  private URI filesystemRoot;
  private Project project;

  private ZeppelinConfiguration conf;

  public FSNotebookRepo(ZeppelinConfiguration conf, Project project) throws
          IOException {
    this.conf = conf;
    this.project = project;
    try {
      filesystemRoot = new URI(conf.getNotebookDir());
    } catch (URISyntaxException e1) {
      throw new IOException(e1);
    }

    if (filesystemRoot.getScheme() == null) { // it is local path
      try {
        this.filesystemRoot = new URI(new File(
                conf.getRelativeDir(filesystemRoot.getPath())).getAbsolutePath());
      } catch (URISyntaxException e) {
        throw new IOException(e);
      }
    } else {
      this.filesystemRoot = filesystemRoot;
    }
    fsManager = VFS.getManager();
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

  private boolean isDirectory(FileObject fo) throws IOException {
    if (fo == null) {
      return false;
    }
    if (fo.getType() == FileType.FOLDER) {
      return true;
    } else {
      return false;
    }
  }

  private LinkedList<FileObject> getNotes(FileObject root, FileObject proj)
          throws IOException {
    FileObject[] rootchildren = root.getChildren();
    FileObject[] projChildren;
    LinkedList<FileObject> children = new LinkedList<>();
    if (isDirectory(proj)) {
      projChildren = proj.getChildren();
      if (projChildren != null) {
        children = new LinkedList<>(Arrays.asList(
                projChildren));
      }
    }
    // add notes in the root dir that are not project specific ex. tutorials
    for (FileObject f : rootchildren) {
      if (isDirectory(f)) {
        FileObject noteJson = f.resolveFile("note.json", NameScope.CHILD);
        if (noteJson.exists() && !listContainsNote(children, f)) {
          children.add(f);
        }
      }
    }
    
    return children;
  }
  
  private boolean listContainsNote(List<FileObject> list, FileObject note){
    for (FileObject fileObj: list){
      if (fileObj.getName().getBaseName().equals(note.getName().getBaseName())){
        return true;
      }
    }
    return false;
  }

  @Override
  public List<NoteInfo> list() throws IOException {
    FileObject rootDir = getRootDir();
    FileObject projectDir = null;
    if (this.project != null) {
      projectDir = rootDir.resolveFile(this.project.getName(),
            NameScope.CHILD);
    }
    

    LinkedList<FileObject> children = getNotes(rootDir, projectDir);

    List<NoteInfo> infos = new LinkedList<>();
    for (FileObject f : children) {
      String fileName = f.getName().getBaseName();
      if (f.isHidden()
              || fileName.startsWith(".")
              || fileName.startsWith("#")
              || fileName.startsWith("~")) {
        // skip hidden, temporary files
        continue;
      }

      if (!isDirectory(f)) {
        // currently single note is saved like, [NOTE_ID]/note.json.
        // so it must be a directory
        continue;
      }

      NoteInfo info = null;

      try {
        info = getNoteInfo(f);
        if (info != null) {
          infos.add(info);
        }
      } catch (IOException e) {
        logger.error("Can't read note " + f.getName().toString(), e);
      }
    }

    return infos;
  }

  private Note getNote(FileObject noteDir) throws IOException {
    if (!isDirectory(noteDir)) {
      throw new IOException(noteDir.getName().toString() + " is not a directory");
    }

    FileObject noteJson = noteDir.resolveFile("note.json", NameScope.CHILD);
    if (!noteJson.exists()) {
      throw new IOException(noteJson.getName().toString() + " not found");
    }

    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setPrettyPrinting();
    Gson gson = gsonBuilder.create();

    FileContent content = noteJson.getContent();
    InputStream ins = content.getInputStream();
    String json = IOUtils.toString(ins, conf.getString(
            ConfVars.ZEPPELIN_ENCODING));
    ins.close();

    Note note = gson.fromJson(json, Note.class);
//    note.setReplLoader(replLoader);
//    note.jobListenerFactory = jobListenerFactory;

    for (Paragraph p : note.getParagraphs()) {
      if (p.getStatus() == Status.PENDING || p.getStatus() == Status.RUNNING) {
        p.setStatus(Status.ABORT);
      }
    }

    return note;
  }

  private NoteInfo getNoteInfo(FileObject noteDir) throws IOException {
    Note note = getNote(noteDir);
    return new NoteInfo(note);
  }

  @Override
  public Note get(String noteId) throws IOException {
    FileObject rootDir = fsManager.resolveFile(getPath("/"));
    FileObject projectDir = rootDir.resolveFile(this.project.getName(),
            NameScope.CHILD);
    FileObject noteDir = projectDir.resolveFile(noteId, NameScope.CHILD);
           
    if (noteDir.exists() && isDirectory(noteDir)) {
      return getNote(noteDir);
    }

    noteDir = rootDir.resolveFile(noteId, NameScope.CHILD);
    return getNote(noteDir);
  }

  private FileObject getRootDir() throws IOException {
    FileObject rootDir = fsManager.resolveFile(getPath("/"));

    if (!rootDir.exists()) {
      throw new IOException("Root path does not exists");
    }

    if (!isDirectory(rootDir)) {
      throw new IOException("Root path is not a directory");
    }

    return rootDir;
  }

  @Override
  public void save(Note note) throws IOException {
    GsonBuilder gsonBuilder = new GsonBuilder();
    gsonBuilder.setPrettyPrinting();
    Gson gson = gsonBuilder.create();
    String json = gson.toJson(note);

    FileObject rootDir = getRootDir();

    FileObject projectDir = rootDir.resolveFile(this.project.getName(),
            NameScope.CHILD);
    if (!projectDir.exists()) {
      projectDir.createFolder();
    }
    if (!isDirectory(projectDir)) {
      throw new IOException(projectDir.getName().toString()
              + " is not a directory");
    }

    FileObject noteDir = projectDir.resolveFile(note.id(), NameScope.CHILD);

    if (!noteDir.exists()) {
      noteDir.createFolder();
    }
    if (!isDirectory(noteDir)) {
      throw new IOException(noteDir.getName().toString() + " is not a directory");
    }

    FileObject noteJson = noteDir.resolveFile("note.json", NameScope.CHILD);
    // false means not appending. creates file if not exists
    OutputStream out = noteJson.getContent().getOutputStream(false);
    out.write(json.getBytes(conf.getString(ConfVars.ZEPPELIN_ENCODING)));
    out.close();
  }

  @Override
  public void remove(String noteId) throws IOException {
    FileObject rootDir = fsManager.resolveFile(getPath("/"));
    FileObject projectDir = rootDir.resolveFile(this.project.getName(),
            NameScope.CHILD);
    if (!projectDir.exists() || !isDirectory(projectDir)) {
      // no project dir
      return;
    }
    FileObject noteDir = projectDir.resolveFile(noteId, NameScope.CHILD);

    if (!noteDir.exists()) {
      // nothing to do
      return;
    }

    if (!isDirectory(noteDir)) {
      // it is not look like zeppelin note savings
      throw new IOException("Can not remove " + noteDir.getName().toString());
    }

    noteDir.delete(Selectors.SELECT_SELF_AND_CHILDREN);
  }

}
