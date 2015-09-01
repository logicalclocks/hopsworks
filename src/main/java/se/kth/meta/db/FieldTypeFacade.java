package se.kth.meta.db;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.FieldType;

/**
 *
 * @author vangelis
 */
@Stateless
public class FieldTypeFacade extends AbstractFacade<FieldType> {

  private static final Logger logger = Logger.getLogger(FieldTypeFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public FieldTypeFacade() {
    super(FieldType.class);
  }

  public List<FieldType> loadFieldTypes() {

    String queryString = "FieldType.findAll";
    Query query = this.em.createNamedQuery(queryString);

    return query.getResultList();
  }
}