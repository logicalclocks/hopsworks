package se.kth.kthfsdashboard.bbc;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.alert.Alert.Severity;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Stateless
public class SampleEjb {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public SampleEjb() {
    }

    public List<Sample> findAll() {
        TypedQuery<Sample> query = em.createNamedQuery("Samples.findAll", Sample.class);
        return query.getResultList();
    }
    
    public void persistSample(Sample sample) {
        em.persist(sample);
    }
    
    public void removeSample(Sample sample) {
       em.remove(sample);
    }
    
}
