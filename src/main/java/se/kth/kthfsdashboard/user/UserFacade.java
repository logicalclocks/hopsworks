/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.bbc.security.ua.model.People;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@Stateless
public class UserFacade extends AbstractFacade<Username> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public UserFacade() {
        super(Username.class);
    }

    @Override
    public List<Username> findAll() {
        TypedQuery<Username> query = em.createNamedQuery("Username.findAll", Username.class);
        return query.getResultList();
    }

    public List<Username> findAllByName() {
        TypedQuery<Username> query = em.createNamedQuery("Username.findAllByName", Username.class);
        return query.getResultList();
    }

    public List<Username> findAllUsers() {
        Query query = em.createNativeQuery("SELECT * FROM USERS", Username.class);
        return query.getResultList();
    }

    public List<Username> filterUsersBasedOnStudy(String name) {

        Query query = em.createNativeQuery("SELECT * FROM USERS WHERE email NOT IN (SELECT team_member FROM StudyTeam WHERE name=?)", Username.class).setParameter(1, name);
        return query.getResultList();
//        List<Username> users = query.getResultList();
//        List<Theme> themes = new ArrayList<>();
//        int i = 0;
//        for (Username u : users) {
//            themes.add(new Theme(i, u.getName(), u.getEmail()));
//            System.out.println(themes + ", ");
//        }
//        return themes;
    }

    public void persist(Username user) {
        em.persist(user);
    }

    public void update(Username user) {
        em.merge(user);
    }

    public void removeByEmail(String email) {
        Username user = findByEmail(email);
        if (user != null) {
            em.remove(user);
        }
    }

    @Override
    public void remove(Username user) {
        if (user != null && user.getEmail() != null && em.contains(user)) {
            em.remove(user);
        }
    }

    public Username findByEmail(String email) {
        return em.find(Username.class, email);
    }

    public Username findByName(String name) {
        return em.find(Username.class, name);
    }

    public void detach(Username user) {
        em.detach(user);
    }
    
    /**
    * Get all users with STATUS = status.
    */
    public List<Username> findAllByStatus(int status) {
        TypedQuery<Username> query = em.createNamedQuery("Username.findAllByStatus", Username.class);
        query.setParameter("status", status);
        return query.getResultList();
    }
}
