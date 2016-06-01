package se.kth.bbc.jobs.jobhistory;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;


@Stateless
public class YarnAppResultFacade extends AbstractFacade<YarnAppResult> {


  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public YarnAppResultFacade() {
    super(YarnAppResult.class);
  }

  @Override
  public List<YarnAppResult> findAll() {
    TypedQuery<YarnAppResult> query = em.createNamedQuery("YarnAppResult.findAll",
            YarnAppResult.class);
    return query.getResultList();
  }
  
  public List<YarnAppResult> findAllHistory() {
    Query query = em.createNativeQuery("SELECT * FROM hopsworks.yarn_app_result",
            YarnAppResult.class);
    return query.getResultList();
  }

  public YarnAppResult findAllByName(String id) {
        try{
            return em.createNamedQuery("YarnAppResult.findById",
            YarnAppResult.class).setParameter("id", id).
            getSingleResult();
        }
        catch (NoResultException e){
            return null;
        }
  }

  public YarnAppResult findByUsername(String username) {
        try{
            return em.createNamedQuery("YarnAppResult.findByUsername",
            YarnAppResult.class).setParameter("username", username).
            getSingleResult();
        }
        catch (NoResultException e){
            return null;
        }
  }
  
   public YarnAppResult findByStartTime(String startTime) {
        try{
            return em.createNamedQuery("YarnAppResult.findByStartTime",
            YarnAppResult.class).setParameter("startTime", startTime).
            getSingleResult();
        }
        catch (NoResultException e){
            return null;
        }
  }
  

  public void update(YarnAppResult yarnApp) {
    em.merge(yarnApp);
  }

}
