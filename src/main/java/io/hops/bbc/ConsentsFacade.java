package io.hops.bbc;


import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.ws.rs.core.Response;
import se.kth.hopsworks.controller.ResponseMessages;
import se.kth.hopsworks.rest.AppException;
import se.kth.kthfsdashboard.user.AbstractFacade;

@Stateless
public class ConsentsFacade extends AbstractFacade<Consents> {

  private static final Logger logger = Logger.getLogger(ConsentsFacade.class.
      getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public ConsentsFacade() {
    super(Consents.class);
  }

  public List<Consents> findAllInProject(int projectId) throws AppException {
     TypedQuery<Consents> q = em.createNamedQuery("Consents.findByProjectId",
            Consents.class);
    q.setParameter("id", projectId);
    return q.getResultList();
  }
  
  public void persistConsent(Consents consent) throws AppException {
    try {
      em.persist(consent);
    } catch (EntityExistsException ex) {
      throw new AppException(Response.Status.CONFLICT.getStatusCode(), ResponseMessages.CONSENT_ALREADY_EXISTS);
    } catch (IllegalArgumentException ex) {
      throw new AppException(Response.Status.BAD_REQUEST.getStatusCode(), ResponseMessages.FILE_NOT_FOUND);      
    }
  }  
  
}
