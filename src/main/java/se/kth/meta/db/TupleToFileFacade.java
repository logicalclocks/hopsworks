package se.kth.meta.db;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.TupleToFile;
import se.kth.meta.exception.DatabaseException;

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

  public List<TupleToFile> getTuplesByInode(int inodeid) throws
          DatabaseException {

    String queryString = "TupleToFile.findByInodeid";

    Query query = this.em.createNamedQuery(queryString);
    return query.getResultList();
  }

  public int addTupleToFile(TupleToFile ttf) throws DatabaseException {

    this.em.persist(ttf);
    this.em.flush();

    return ttf.getId();
  }

}
