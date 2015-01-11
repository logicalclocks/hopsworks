package se.kth.bbc.study.samples;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
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

  public List<Samplecollection> findByStudyname(String studyname) {
    TypedQuery<Samplecollection> q = em.createNamedQuery("Samplecollection.findByStudyname",
            Samplecollection.class);
    q.setParameter("studyname", studyname);
    return q.getResultList();
  }
  
  public Samplecollection findById(String id){
    TypedQuery<Samplecollection> q = em.createNamedQuery("Samplecollection.findById",
            Samplecollection.class);
    q.setParameter("id", id);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  /*TODO: this must be the most idiotic update function ever written... But:
   * Writing
   *  em.merge(meta);
   * for some reason fails. The resulting queries insert study_id null in the STUDY_DESIGN table.
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
  
  public void persist(Samplecollection s){
    em.persist(s);
  }

}
