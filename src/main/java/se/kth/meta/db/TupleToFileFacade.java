package se.kth.meta.db;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import se.kth.kthfsdashboard.user.AbstractFacade;
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
//    String query = "TupleToFile.findByTupleid";
//    Query q = this.em.createNamedQuery(query);
//    q.setParameter("tupleid", tupleid);
//
//    List<TupleToFile> result = q.getResultList();
//
//    return result.get(0);
  }

  public int addTupleToFile(TupleToFile ttf) throws DatabaseException {

    this.em.persist(ttf);
    this.em.flush();

    return ttf.getId();
  }

}
