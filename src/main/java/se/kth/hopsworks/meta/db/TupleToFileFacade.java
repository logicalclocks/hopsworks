package se.kth.hopsworks.meta.db;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.bbc.project.fb.Inode;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.hopsworks.meta.entity.TupleToFile;
import se.kth.hopsworks.meta.exception.DatabaseException;

/**
 *
 * @author vangelis
 */
@Stateless
public class TupleToFileFacade extends AbstractFacade<TupleToFile> {

  private static final Logger logger = Logger.getLogger(TupleToFileFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public TupleToFileFacade() {
    super(TupleToFile.class);
  }

  public TupleToFile getTupletofile(int tupleid) throws DatabaseException {

    return this.em.find(TupleToFile.class, tupleid);
  }

  public List<TupleToFile> getTuplesByInodeId(Integer parentId, String inodeName)
          throws DatabaseException {

    String queryString = "TupleToFile.findByInodeid";

    Query query = this.em.createNamedQuery(queryString);
    query.setParameter("parentid", parentId);
    query.setParameter("name", inodeName);
    return query.getResultList();
  }

  public int addTupleToFile(TupleToFile ttf) throws DatabaseException {

    this.em.persist(ttf);
    this.em.flush();

    return ttf.getId();
  }

  /**
   * Deletes a tupleToFile entity. If the object is an
   * unmanaged entity it has to be merged to become managed so that delete
   * can cascade down its associations if necessary
   * <p>
   *
   * @param ttf
   * @throws se.kth.hopsworks.meta.exception.DatabaseException
   */
  public void deleteTTF(TupleToFile ttf) throws DatabaseException {

    TupleToFile tf = this.contains(ttf) ? ttf : this.getTupletofile(ttf.getId());

    if (this.em.contains(tf)) {
      this.em.remove(tf);
    } else {
      //if the object is unmanaged it has to be managed before it is removed
      this.em.remove(this.em.merge(tf));
    }
  }

  /**
   * Checks if a tupleToFile instance is a managed entity
   * <p>
   * @param ttf
   * @return
   */
  public boolean contains(TupleToFile ttf) {
    return this.em.contains(ttf);
  }
}
