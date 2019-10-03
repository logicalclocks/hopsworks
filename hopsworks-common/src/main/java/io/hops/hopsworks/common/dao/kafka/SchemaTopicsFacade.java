package io.hops.hopsworks.common.dao.kafka;

import io.hops.hopsworks.common.dao.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Stateless
public class SchemaTopicsFacade extends AbstractFacade<SchemaTopics> {
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  public SchemaTopicsFacade() {
    super(SchemaTopics.class);
  }
  
  public Optional<SchemaTopics> findSchemaByNameAndVersion(String schemaName, Integer schemaVersion) {
    try {
      return Optional.of(find(new SchemaTopicsPK(schemaName, schemaVersion)));
    } catch (NullPointerException e) {
      return Optional.empty();
    }
  }
  
  @Deprecated
  public Optional<SchemaTopics> getSchemaByNameAndVersion(String schemaName, Integer schemaVersion) {
    try {
      return Optional.of(em.createNamedQuery("SchemaTopics.findByNameAndVersion", SchemaTopics.class)
        .setParameter("name", schemaName)
        .setParameter("version", schemaVersion)
        .getSingleResult());
    } catch (NoResultException e) {
      return Optional.empty();
    }
  }
}
