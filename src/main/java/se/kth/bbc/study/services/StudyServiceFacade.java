package se.kth.bbc.study.services;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class StudyServiceFacade extends AbstractFacade<StudyServices> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public StudyServiceFacade() {
    super(StudyServices.class);
  }

  public List<String> findEnabledServicesForStudy(String studyname) {
    //TODO: why does this return String?
    Query q = em.createNamedQuery("StudyServices.findServicesByStudy",
            StudyServiceEnum.class);
    q.setParameter("study", studyname);
    return q.getResultList();
  }

  public void persistServicesForStudy(String studyname,
          StudyServiceEnum[] services) {
    //TODO: Probably not the most efficient way of doing this.
    for(StudyServices se:getAllStudyServicesForStudy(studyname)){
      if(!arrayContains(services, StudyServiceEnum.valueOf(se.studyServicePK.getService()))){
        em.remove(se);
      }
    }
    for(StudyServiceEnum se:services){
      em.persist(new StudyServices(studyname,se.name()));
    }
  }

  public void addServiceForStudy(String studyname, StudyServiceEnum service) {
    if (!findEnabledServicesForStudy(studyname).contains(service)) {
      StudyServices ss = new StudyServices(studyname, service.name());
      em.persist(ss);
    }
  }

  public void removeServiceForStudy(String studyname, StudyServiceEnum service) {
    StudyServices c = em.find(StudyServices.class, new StudyServicePK(studyname,
            service.name()));
    if (c != null) {
      em.remove(c);
    }
  }

  private boolean arrayContains(StudyServiceEnum[] array, StudyServiceEnum val) {
    for (StudyServiceEnum sse : array) {
      if (sse == val) {
        return true;
      }
    }
    return false;
  }

  private List<StudyServices> getAllStudyServicesForStudy(String studyname) {
    Query q = em.createNamedQuery("StudyServices.findByStudy",
            StudyServices.class);
    q.setParameter("study", studyname);
    return q.getResultList();
  }

}
