package se.kth.bbc.project.samples;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.bbc.project.Project;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class SamplecollectionFacade extends AbstractFacade<Samplecollection> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  public SamplecollectionFacade() {
    super(Samplecollection.class);
  }

  public List<Samplecollection> findByProject(Project project) {
    TypedQuery<Samplecollection> q = em.createNamedQuery(
            "Samplecollection.findByProject",
            Samplecollection.class);
    q.setParameter("project", project);
    return q.getResultList();
  }

  public Samplecollection findById(String id) {
    TypedQuery<Samplecollection> q = em.createNamedQuery(
            "Samplecollection.findById",
            Samplecollection.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /*
   * TODO: this must be the most idiotic update function ever written... But:
   * Writing
   * em.merge(meta);
   * for some reason fails. The resulting queries insert project_id null in the
   * PROJECT_DESIGN table.
   * I have not yet figured out why...
   */
  public void update(Samplecollection meta) {
    Samplecollection old = findById(meta.getId());
    if (old != null) {
      em.remove(old);
      em.flush();
    }
    em.persist(meta);
  }

  public void persist(Samplecollection s) {
    em.persist(s);
  }

  //TODO: make the queries here more efficient: do not fetch an entire list!
  /**
   * Check if a collection with this id already exists.
   * <p>
   * @param id
   * @return
   */
  public boolean existsCollectionWithId(String id) {
    TypedQuery<Samplecollection> q = em.createNamedQuery(
            "Samplecollection.findById", Samplecollection.class);
    q.setParameter("id", id);
    return q.getResultList().size() > 0;
  }

  /**
   * Check if a collection with this acronym already exists.
   * <p>
   * @param acronym
   * @return
   */
  public boolean existsCollectionWithAcronym(String acronym) {
    TypedQuery<Samplecollection> q = em.createNamedQuery(
            "Samplecollection.findByAcronym", Samplecollection.class);
    q.setParameter("acronym", acronym);
    return q.getResultList().size() > 0;
  }

}
