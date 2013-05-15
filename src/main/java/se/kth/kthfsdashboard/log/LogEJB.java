package se.kth.kthfsdashboard.log;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.*;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Stateless
public class LogEJB {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public LogEJB() {
    }

    public List<Log> findLogs() {
        TypedQuery<Log> query = em.createNamedQuery("findAll", Log.class);
        return query.getResultList();
    }

    public Number findLatestLogTime(String host) {

        Query query = em.createNamedQuery("findLatestLogTime", Log.class).setParameter("host", host);
        return (Number) query.getSingleResult();
    }

    public List<Log> findLatestLogForPlugin(String host, String plugin) {

        TypedQuery<Log> query = em.createNamedQuery("findLatestLogForPlugin", Log.class).setParameter("host", host).setParameter("plugin", plugin);
        return query.getResultList();
    }

    public Log findLatestLogForPluginAndType(String host, String plugin, String type) {

        TypedQuery<Log> query = em.createNamedQuery("findLatestLogForPluginAndType", Log.class).setParameter("host", host).setParameter("plugin", plugin).setParameter("type", type);
        return query.getSingleResult();
    }

    public List<Log> findLogForPluginAndTypeAtTime(long time, String host, String plugin, String type) {

        TypedQuery<Log> query = em.createNamedQuery("findLogForPluginAndTypeAtTime", Log.class).setParameter("host", host).setParameter("plugin", plugin).setParameter("type", type).setParameter("time", time);
        return query.getResultList();
    }

    public Number findNumberOfCpu(String host) {

        Query query = em.createNamedQuery("findNumberOfCpu", Log.class).setParameter("host", host);
        return (Number) query.getSingleResult();
    }

    public List<Log> findLogsLastMin(String host, int time) {

        Calendar start = new GregorianCalendar();
        start.add(Calendar.MINUTE, -time);

        TypedQuery<Log> query = em.createNamedQuery("findLastMin", Log.class).setParameter("start", start.getTime(), TemporalType.TIMESTAMP).setParameter("machine", host);
        return query.getResultList();
    }

    public List<Log> findLatestLog(String host, String plugin, String type) {

        TypedQuery<Log> query = em.createNamedQuery("findLastestLog", Log.class).setParameter("host", host).setParameter("plugin", plugin).setParameter("type", type);
//         TypedQuery<Log> query = em.createNamedQuery("findLastestLog", Log.class).setParameter("host", host).setParameter("plugin", plugin).setParameter("type", type);
        
//        em.createNativeQuery(type, null)
        return query.getResultList();
    }

    public Log addLog(Log log) {

        this.em.persist(log);
//        this.em.flush();
        return log;
    }

    public void addAllLogs(HashSet<Log> logs) {

        for (Log log : logs) {
            this.em.persist(log);
//            this.em.flush();
        }

    }
}
