package se.kth.bbc.study.services;

import java.util.ArrayList;
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

  public List<StudyServiceEnum> findEnabledServicesForStudy(String studyname) {
    //TODO: why does this return String?
    Query q = em.createNamedQuery("StudyServices.findServicesByStudy",
            StudyServiceEnum.class);
    q.setParameter("study", studyname);
    return q.getResultList();
  }

  //TODO: write tests for this
  public void persistServicesForStudy(String studyname,
          StudyServiceEnum[] services) {
    //TODO: use copy instead
    List<StudyServices> newSrvs = new ArrayList<>(services.length);
    List<StudyServices> toPersist = new ArrayList<>(services.length);
    for(StudyServiceEnum sse:services){
      StudyServices c = new StudyServices(studyname,sse);
      newSrvs.add(c);
      toPersist.add(c);
    }    
    List<StudyServices> current = getAllStudyServicesForStudy(studyname);
    
    toPersist.removeAll(current);
    current.removeAll(newSrvs);
    
    for(StudyServices se: toPersist){
      em.persist(se);
    }
    for(StudyServices se: current){
      em.remove(se);
    }
  }

  public void addServiceForStudy(String studyname, StudyServiceEnum service) {
    if (!findEnabledServicesForStudy(studyname).contains(service)) {
      StudyServices ss = new StudyServices(studyname, service);
      em.persist(ss);
    }
  }

  public void removeServiceForStudy(String studyname, StudyServiceEnum service) {
    StudyServices c = em.find(StudyServices.class, new StudyServicePK(studyname,
            service));
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

  public List<StudyServices> getAllStudyServicesForStudy(String studyname) {
    Query q = em.createNamedQuery("StudyServices.findByStudy",
            StudyServices.class);
    q.setParameter("study", studyname);
    return q.getResultList();
  }

}
