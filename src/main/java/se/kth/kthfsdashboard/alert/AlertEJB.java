package se.kth.kthfsdashboard.alert;

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
public class AlertEJB {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public AlertEJB() {
    }

    public List<Alert> findAll(Date fromDate, Date toDate) {

        TypedQuery<Alert> query = em.createNamedQuery("Alerts.findAll", Alert.class)
                .setParameter("fromdate", fromDate).setParameter("todate", toDate) ;
        return query.getResultList();
    }
    
    public void persistAlert(Alert alert) {
        em.persist(alert);
    }
    
    public void removeAlert(Alert alert) {
       em.remove(em.merge(alert));
    }
    
    public void removeAllAlerts() {
       em.createNamedQuery("Alerts.removeAll").executeUpdate();
    }    
}
