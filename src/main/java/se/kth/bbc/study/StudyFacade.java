package se.kth.bbc.study;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import org.ocpsoft.logging.Logger;
import se.kth.kthfsdashboard.user.AbstractFacade;

/**
 *
 * @author stig
 */
@Stateless
public class StudyFacade extends AbstractFacade<TrackStudy>{
    
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    
    public StudyFacade(){
        super(TrackStudy.class);
    }
    
    //TODO: somehow studies are persisted somewhere else. Find out where and either merge
    //classes or move here.
    
    public TrackStudy getStudyByName(String studyName){
        TypedQuery<TrackStudy> query = em.createNamedQuery("TrackStudy.findByName", TrackStudy.class);
        query.setParameter("name",studyName);
        try{
            TrackStudy t = query.getSingleResult();
            return t;
        }catch(NoResultException e){
            Logger.getLogger(StudyFacade.class.getName()).error("No result. Should not happen.",e);
            return null;
        }
    }
    
    
}
