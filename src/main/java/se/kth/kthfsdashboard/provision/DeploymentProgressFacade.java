/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import java.util.List;
import java.util.Set;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.jclouds.compute.domain.NodeMetadata;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.kthfsdashboard.virtualization.clusterparser.Baremetal;
import se.kth.kthfsdashboard.virtualization.clusterparser.BaremetalGroup;
import se.kth.kthfsdashboard.virtualization.clusterparser.Cluster;
import se.kth.kthfsdashboard.virtualization.clusterparser.NodeGroup;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Stateless
public class DeploymentProgressFacade extends AbstractFacade<NodeProgression> {

    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    public DeploymentProgressFacade() {
        super(NodeProgression.class);

    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public List<NodeProgression> findAll() {
        TypedQuery<NodeProgression> query = em.createNamedQuery("NodeProgression.findAll", NodeProgression.class);
        return query.getResultList();
    }

    public void persistNodeProgress(NodeProgression progress) {
        em.persist(progress);
    }

    public void removeNodeProgress(NodeProgression progress) {
        em.remove(em.merge(progress));
    }

    public void updateProgress(NodeProgression progress) {
        em.merge(progress);
    }

    public void createProgress(Cluster cluster) {

        NodeProgression progress;
        for (NodeGroup group : cluster.getNodes()) {
            for (int i = 0; i < group.getNumber(); i++) {
                progress = new NodeProgression();
                progress.setCluster(cluster.getName());
                progress.setNodeId(group.getSecurityGroup() + i);
                progress.setPhase(DeploymentPhase.WAITING.toString());
                progress.setRole(group.getRoles().toString());
                persistNodeProgress(progress);
            }
        }
    }

    public void createProgress(Baremetal cluster) {

        NodeProgression progress;
        for (BaremetalGroup group : cluster.getNodes()) {
            for (int i = 0; i < group.getNumber(); i++) {
                progress = new NodeProgression();
                progress.setCluster(cluster.getName());
                progress.setNodeId(group.getHosts().get(i));
                progress.setPhase(DeploymentPhase.WAITING.toString());
                progress.setRole(group.getRoles().toString());
                persistNodeProgress(progress);
            }
        }
    }

    public void initializeCreateGroup(String group, int number) throws Exception {

        for (int i = 0; i < number; i++) {
            TypedQuery<NodeProgression> query =
                    em.createNamedQuery("NodeProgression.findNodeByNodeID", NodeProgression.class)
                    .setParameter("nodeId", group + i);
            try {
                List<NodeProgression> values = query.getResultList();
                for (NodeProgression node : values) {
                    node.setPreviousPhase(node.getPhase());
                    node.setPhase(DeploymentPhase.CREATION.toString());
                    updateProgress(node);
                }

            } catch (NoResultException ex) {
                throw new Exception("NoResultException");
            }
        }
    }

    public void initializeHostProgress(String hostIP) throws Exception {
        TypedQuery<NodeProgression> query =
                em.createNamedQuery("NodeProgression.findNodeByNodeID", NodeProgression.class)
                .setParameter("nodeId", hostIP);
        try {
            NodeProgression value = query.getSingleResult();
            value.setPreviousPhase(value.getPhase());
            value.setPhase(DeploymentPhase.CREATION.toString());
            updateProgress(value);
        } catch (NoResultException E) {
            throw new Exception("NoResultException");
        }
    }

    public void updateCreationCluster(String clusterName) throws Exception {
        TypedQuery<NodeProgression> query =
                em.createNamedQuery("NodeProgression.AllNodeByClusterName", NodeProgression.class)
                .setParameter("clusterName", clusterName);
        try {
            List<NodeProgression> values = query.getResultList();
            for (NodeProgression node : values) {
                node.setPreviousPhase(node.getPhase());
                node.setPhase(DeploymentPhase.CREATED.toString());
                updateProgress(node);
            }

        } catch (NoResultException ex) {
            throw new Exception("NoResultException");
        }
    }

    public void updateCreateProgress(String group, String nodeID, int i) throws Exception {

        TypedQuery<NodeProgression> query =
                em.createNamedQuery("NodeProgression.findNodeByNodeID", NodeProgression.class)
                .setParameter("nodeId", group + i);
        try {

            NodeProgression node = query.getSingleResult();
            node.setNodeId(nodeID);
            node.setPreviousPhase(node.getPhase());
            node.setPhase(DeploymentPhase.CREATED.toString());

            updateProgress(node);

        } catch (NoResultException ex) {
            throw new Exception("NoResultException");
        }

    }

    public void updatePhaseProgress(Set<NodeMetadata> nodes, DeploymentPhase phase)
            throws Exception {
        for (NodeMetadata node : nodes) {
            System.out.println("Updating the node with node id: " + node.getId());
            TypedQuery<NodeProgression> query =
                    em.createNamedQuery("NodeProgression.findNodeByNodeID", NodeProgression.class)
                    .setParameter("nodeId", node.getId());
            try {

                NodeProgression temp = query.getSingleResult();
                temp.setPreviousPhase(temp.getPhase());
                temp.setPhase(phase.toString());
                System.out.println(temp);
                updateProgress(temp);

            } catch (NoResultException ex) {
                throw new Exception("NoResultException");
            }
        }




    }

    public void deleteAllProgress() {
        Query query = em.createQuery("DELETE c FROM NodeProgression");
        int result = query.executeUpdate();
    }
}
