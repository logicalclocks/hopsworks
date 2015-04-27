
package se.kth.meta.listener;


import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import se.kth.meta.db.Dbao;
import se.kth.meta.entity.User;

/**
 *
 * @author Vangelis
 */
//@WebListener
public class SessionListener implements ServletContextListener, HttpSessionListener,
        ServletRequestListener {

    private Dbao dbao;
    private static final String ATTRIBUTE_NAME = "listener.SessionListener";
    private Map<HttpSession, String> sessions = new ConcurrentHashMap<>();
    private static Map<String, User> usersonline = new ConcurrentHashMap<>();
    private List<HttpSession> activeSessions = new LinkedList<>();

    @Override
    public void contextInitialized(ServletContextEvent event) {
        System.err.println("METAHOPS: CONTEXT INITIALIZED");
        ServletContext context = event.getServletContext();

        context.setAttribute(ATTRIBUTE_NAME, this);

//        try {
//            Dbao dbao = new Dbao("localhost", 3306, "metahops", "metahops", "metahops");
//            context.setAttribute("db", this.dbao);
//        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | SQLException e) {
//            System.err.println("SessionListener: " + e.getMessage());
//        }
    }

    @Override
    public void requestInitialized(ServletRequestEvent event) {
        System.out.println("METAHOPS: {SESSION LISTENER}: REQUEST INITIALIZED --- ACTIVE SESSIONS " + sessions.size());
        HttpServletRequest request = (HttpServletRequest) event.getServletRequest();
        HttpSession session = request.getSession();

        if (session.isNew()) {
            sessions.put(session, request.getRemoteAddr());
            System.err.println("METAHOPS: {SESSION LISTENER}: SESSION RECORDED --- ACTIVE REQUESTS " + sessions.size());
        }
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        // NOOP. No logic needed.
        System.out.println("METAHOPS: {SESSION LISTENER}: REQUEST DESROYED");
    }

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        System.out.println("METAHOPS: SESSION CREATED");
        HttpSession session = event.getSession();

        if (session.isNew()) {
            System.err.println("METAHOPS: {SESSION LISTENER}: RECORDING SESSION");
            activeSessions.add(event.getSession());
        }
        System.out.println("METAHOPS: {SESSION LISTENER}: ACTIVE SESSIONS " + activeSessions.size());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        sessions.remove(event.getSession());
        System.err.println("METAHOPS: {SESSION LISTENER}: SESSION DESTROYED --- ACTIVE REQUESTS " + sessions.size());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {

        System.out.println("METAHOPS: {SESSION LISTENER}: CONTEXT DESTROYED");
//        try {
//            this.dbao.shutdown();
//        } catch (NullPointerException e) {
//            //in case the database was not initialized
//            System.err.println("METAHOPS: Database was not initialized " + e.getMessage());
//        } catch (DatabaseException ex) {
//            Logger.getLogger(SessionListener.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    public static SessionListener getInstance(ServletContext context) {
        return (SessionListener) context.getAttribute(ATTRIBUTE_NAME);
    }

//    public Map<HttpSession, String> getSessions() {
//        return this.sessions;
//    }
    public List<HttpSession> getActiveSessions() {
        return this.activeSessions;
    }

    public Map<String, User> getUsersOnline() {
        return usersonline;
    }

    public static void addUser(User user) {
        usersonline.put(user.getEmail(), user);
    }

    public static boolean containsUser(User user) {
        return usersonline.containsKey(user.getEmail());
    }

    public int getCount(String remoteAddr) {
        return Collections.frequency(sessions.values(), remoteAddr);
    }

}
