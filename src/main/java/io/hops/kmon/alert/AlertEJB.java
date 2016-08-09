package io.hops.kmon.alert;

import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import io.hops.kmon.alert.Alert.Provider;
import io.hops.kmon.alert.Alert.Severity;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 * 
 * See: https://abhirockzz.wordpress.com/2015/02/19/valid-cdi-scoped-for-session-ejb-beans/
 */
@Stateless
public class AlertEJB {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public AlertEJB() {
    }

    public List<Alert> find(Date fromDate, Date toDate) {

        TypedQuery<Alert> query = em.createNamedQuery("Alerts.findAll", Alert.class)
                .setParameter("fromdate", fromDate).setParameter("todate", toDate) ;
        return query.getResultList();
    }
    
    public List<Alert> find(Date fromDate, Date toDate, Severity severity) {
        TypedQuery<Alert> query = em.createNamedQuery("Alerts.findBy-Severity", Alert.class)
                .setParameter("fromdate", fromDate).setParameter("todate", toDate)
                .setParameter("severity", severity.toString());
        return query.getResultList();
    }
    
    public List<Alert> find(Date fromDate, Date toDate, Provider provider) {
        TypedQuery<Alert> query = em.createNamedQuery("Alerts.findBy-Provider", Alert.class)
                .setParameter("fromdate", fromDate).setParameter("todate", toDate)
                .setParameter("provider", provider.toString());
        return query.getResultList();
    }
    
    public List<Alert> find(Date fromDate, Date toDate, Provider provider, Severity severity) {
        TypedQuery<Alert> query = em.createNamedQuery("Alerts.findBy-Provider-Severity", Alert.class)
                .setParameter("fromdate", fromDate).setParameter("todate", toDate)
                .setParameter("provider", provider).setParameter("severity", severity);
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
