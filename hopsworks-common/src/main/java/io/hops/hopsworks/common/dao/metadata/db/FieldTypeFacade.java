package io.hops.hopsworks.common.dao.metadata.db;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.metadata.FieldType;

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
