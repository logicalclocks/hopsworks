/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;


@Stateless
public class YarnAppHeuristicResultDetailsFacade extends AbstractFacade<YarnAppHeuristicResultDetails>{
    
  
  private static final Logger logger = Logger.getLogger(YarnAppHeuristicResultDetailsFacade.class.
      getName());

  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  
  public YarnAppHeuristicResultDetailsFacade() {
    super(YarnAppHeuristicResultDetails.class);
  }
  
  
  public String searchByIdAndName(int yarnAppHeuristicResultId, String name){
        TypedQuery<YarnAppHeuristicResultDetails> q = em.createNamedQuery("YarnAppHeuristicResultDetails.findByIdAndName",
            YarnAppHeuristicResultDetails.class);
        q.setParameter("yarnAppHeuristicResultId", yarnAppHeuristicResultId);
        q.setParameter("name", name);
        
        YarnAppHeuristicResultDetails result = q.getSingleResult();
        
        return result.getValue();
        
    }
}
