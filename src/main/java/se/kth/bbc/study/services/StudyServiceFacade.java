package se.kth.bbc.study.services;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.bbc.study.Study;
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

  public List<StudyServiceEnum> findEnabledServicesForStudy(Study study) {
    //TODO: why does this return String?
    Query q = em.createNamedQuery("StudyServices.findServicesByStudy",
            StudyServiceEnum.class);
    q.setParameter("study", study);
    return q.getResultList();
  }

  //TODO: write tests for this
  public void persistServicesForStudy(Study study,
          StudyServiceEnum[] services) {
    //TODO: use copy instead
    List<StudyServices> newSrvs = new ArrayList<>(services.length);
    List<StudyServices> toPersist = new ArrayList<>(services.length);
    for (StudyServiceEnum sse : services) {
      StudyServices c = new StudyServices(study, sse);
      newSrvs.add(c);
      toPersist.add(c);
    }
    List<StudyServices> current = getAllStudyServicesForStudy(study);

    toPersist.removeAll(current);
    current.removeAll(newSrvs);

    for (StudyServices se : toPersist) {
      em.persist(se);
    }
    for (StudyServices se : current) {
      em.remove(se);
    }
  }

  public void addServiceForStudy(Study study, StudyServiceEnum service) {
    if (!findEnabledServicesForStudy(study).contains(service)) {
      StudyServices ss = new StudyServices(study, service);
      em.persist(ss);
    }
  }

  public void removeServiceForStudy(Study study, StudyServiceEnum service) {
    StudyServices c = em.find(StudyServices.class, new StudyServicePK(study.getId(),
            service));
    if (c != null) {
      em.remove(c);
    }
  }

  public List<StudyServices> getAllStudyServicesForStudy(Study study) {
    Query q = em.createNamedQuery("StudyServices.findByStudy",
            StudyServices.class);
    q.setParameter("study", study);
    return q.getResultList();
  }

}
