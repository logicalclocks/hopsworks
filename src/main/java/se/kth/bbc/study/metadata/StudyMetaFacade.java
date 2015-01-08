package se.kth.bbc.study.metadata;

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
public class StudyMetaFacade extends AbstractFacade<StudyMeta> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  public StudyMetaFacade() {
    super(StudyMeta.class);
  }

  public StudyMeta findByStudyname(String studyname) {
    TypedQuery<StudyMeta> q = em.createNamedQuery("StudyMeta.findByStudyname",
            StudyMeta.class);
    q.setParameter("studyname", studyname);
    try {
      return q.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public void update(StudyMeta meta) {
    em.merge(meta);
  }

}
