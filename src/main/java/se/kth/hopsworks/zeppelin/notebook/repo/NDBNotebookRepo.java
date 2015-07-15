
package se.kth.hopsworks.zeppelin.notebook.repo;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import se.kth.bbc.project.Project;

/**
 *
 * @author ermiasg
 */
public class NDBNotebookRepo implements NotebookRepo {

  private static final Logger logger = Logger.getLogger(NDBNotebookRepo.class.
          getName());
  private ZeppelinConfiguration conf;
  private Project project;

  public NDBNotebookRepo(ZeppelinConfiguration conf, Project project) {
    this.conf = conf;
    this.project = project;
  }

  @Override
  public List<NoteInfo> list() throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Note get(String noteId) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void save(Note note) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void remove(String noteId) throws IOException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
