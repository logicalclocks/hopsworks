package se.kth.hopsworks.workflows;

import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class EdgeFacade extends AbstractFacade<Edge> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<Edge> findAll() {
        TypedQuery<Edge> query = em.createNamedQuery("Edge.findAll",
                Edge.class);
        return query.getResultList();
    }

    public Edge findById(EdgePK id) {
        TypedQuery<Edge> query =  em.createNamedQuery("Edge.findById", Edge.class);
        query.setParameter("edgePK", id);
        return query.getSingleResult();
    }

    public EdgeFacade() {
        super(Edge.class);
    }

    public void persist(Edge edge) {
        em.persist(edge);
    }

    public void flush() {
        em.flush();
    }

    public Edge merge(Edge edge) {
        return em.merge(edge);

    }

    public void remove(Edge edge) {
        em.remove(em.merge(edge));
    }

    public Edge refresh(Edge edge) {
        Edge e = findById(edge.getEdgePK());
        em.refresh(e);
        return e;
    }
}
