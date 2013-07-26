/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import java.io.Serializable;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import org.ocpsoft.rewrite.faces.annotation.Phase;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@RequestScoped
public class NodeProgressionController implements Serializable {

    @EJB
    private DeploymentProgressFacade deploymentFacade;
    private List<NodeProgression> nodes;

    /**
     * Creates a new instance of NodeProgressionController
     */
    public NodeProgressionController() {
    }

    @PostConstruct
    public void init() {
        loadNodes();
    }

    public List<NodeProgression> getNodes() {

        loadNodes();

        return nodes;
    }

    public Integer progress(NodeProgression progress) {
        DeploymentPhase currentPhase = DeploymentPhase.fromString(progress.getPhase());
        DeploymentPhase previousPhase = DeploymentPhase.fromString(progress.getPreviousPhase());
        if (currentPhase.equals(DeploymentPhase.CREATION)) {
            return 0;
        } else if (currentPhase.equals(DeploymentPhase.INSTALL)
                || currentPhase.equals(DeploymentPhase.CREATED)) {
            return 33;
        } else if (currentPhase.equals(DeploymentPhase.CONFIGURE)) {
            return 66;
        } else if (currentPhase.equals(DeploymentPhase.COMPLETE)) {
            return 100;
        } else if (currentPhase.equals(DeploymentPhase.WAITING)) {
            if (previousPhase != null &&( previousPhase.equals(DeploymentPhase.INSTALL)
                    ||previousPhase.equals(DeploymentPhase.CREATED))) {
                return 33;
            }
            return 0;
        }
        return 0;
    }

    private void loadNodes() {
        nodes = deploymentFacade.findAll();
    }
}
