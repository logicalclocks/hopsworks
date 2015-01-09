package se.kth.kthfsdashboard.bbc;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

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

    public List<SampleOld> findAll() {
        TypedQuery<SampleOld> query = em.createNamedQuery("Samples.findAll", SampleOld.class);
        return query.getResultList();
    }
    
    public void persistSample(SampleOld sample) {
        em.persist(sample);
    }
    
    public void removeSample(SampleOld sample) {
       em.remove(sample);
    }
    
}
