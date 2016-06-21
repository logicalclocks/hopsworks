/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.jobs.jobhistory;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;


@Stateless
public class YarnAppHeuristicResultFacade extends AbstractFacade<YarnAppHeuristicResult>{
    
  
  private static final Logger logger = Logger.getLogger(YarnAppHeuristicResultFacade.class.
      getName());

  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;
  
  
  @Override
  protected EntityManager getEntityManager() {
    return em;
  }
  
  
  public YarnAppHeuristicResultFacade() {
    super(YarnAppHeuristicResult.class);
  }
  
  
  public Integer searchByIdAndClass(String yarnAppResultId, String heuristicClass){
        TypedQuery<YarnAppHeuristicResult> q = em.createNamedQuery("YarnAppHeuristicResult.findByIdAndHeuristicClass",
            YarnAppHeuristicResult.class);
        q.setParameter("yarnAppResultId", yarnAppResultId);
        q.setParameter("heuristicClass", heuristicClass);
        
        YarnAppHeuristicResult result = q.getSingleResult();
        
        return result.getId();
        
  }
  
  public String searchForSeverity(String yarnAppResultId, String heuristicClass){
    try{
        TypedQuery<YarnAppHeuristicResult> q = em.createNamedQuery("YarnAppHeuristicResult.findByIdAndHeuristicClass",
            YarnAppHeuristicResult.class);
        q.setParameter("yarnAppResultId", yarnAppResultId);
        q.setParameter("heuristicClass", heuristicClass);
        
        YarnAppHeuristicResult result = q.getSingleResult();

        short severity = result.getSeverity();
        switch(severity){
            case 0 :
                return "NONE";
            case 1 :
                return "LOW";
            case 2 :
                return "MODERATE";
            case 3 :
                return "SEVERE";
            case 4 :
                return "CRITICAL";
            default : 
                return "NONE";
        }
    }
    catch(NoResultException e){
        return "UNDEFINED";
    }
  }

}
