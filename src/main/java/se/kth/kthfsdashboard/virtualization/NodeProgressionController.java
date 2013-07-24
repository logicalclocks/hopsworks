/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import org.ocpsoft.rewrite.faces.annotation.Phase;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@RequestScoped
public class NodeProgressionController {

    @EJB
    private DeploymentProgressFacade deploymentFacade;
    private List<NodeProgression> nodes; 

    /**
     * Creates a new instance of NodeProgressionController
     */
    public NodeProgressionController() {
    
    }
    
    @PostConstruct
    public void init(){
        loadNodes();
    }
    
    public List<NodeProgression> getNodes(){
        if(nodes!=null){
            loadNodes();
        }
        return nodes;
    }
    
    public Integer progress(NodeProgression progress){
        DeploymentPhase currentPhase = DeploymentPhase.fromString(progress.getPhase());
        if(currentPhase.equals(DeploymentPhase.CREATION)){
            return 0;
        }
        else if (currentPhase.equals(DeploymentPhase.INSTALL)){
            return 33;
        }
        else if(currentPhase.equals(DeploymentPhase.CONFIGURE)){
            return 66;
        }
        else if(currentPhase.equals(DeploymentPhase.COMPLETE)){
            return 100;
        }
        else{
            return 0;
        }
    }
    
    
    private void loadNodes(){
        nodes = deploymentFacade.findAll();
    }
    
}
