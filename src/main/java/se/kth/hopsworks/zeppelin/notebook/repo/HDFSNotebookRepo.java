package se.kth.hopsworks.zeppelin.notebook.repo;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 *  Backend for storing Notebooks on hdfs
 */
public class HDFSNotebookRepo implements NotebookRepo{
  private static final Logger LOG = LoggerFactory.getLogger(HDFSNotebookRepo.class);

  private DistributedFileSystem dfs;
  private URI filesystemRoot;
  private ZeppelinConfiguration conf;
  
  public HDFSNotebookRepo(ZeppelinConfiguration conf) throws IOException {
    this.conf = conf;
    setNotebookDirectory(conf.getNotebookDir());
  }
  
  
  private void setNotebookDirectory(String notebookDir) throws IOException {
    
  }
  
  @Override
  public List<NoteInfo> list(AuthenticationInfo subject) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Note get(String noteId, AuthenticationInfo subject) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void save(Note note, AuthenticationInfo subject) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void remove(String noteId, AuthenticationInfo subject) throws
          IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void close() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Revision checkpoint(String noteId, String checkpointMsg,
          AuthenticationInfo subject) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Note get(String noteId, Revision rev, AuthenticationInfo subject)
          throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public List<Revision> revisionHistory(String noteId,
          AuthenticationInfo subject) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
}
