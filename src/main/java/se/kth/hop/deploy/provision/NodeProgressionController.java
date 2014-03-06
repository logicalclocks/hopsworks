package se.kth.hop.deploy.provision;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.Serializable;
import java.util.concurrent.Executors;
import javax.annotation.PostConstruct;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.scriptbuilder.domain.OsFamily;

/**
 * Controller which manages the functionality to handle the progress
 * of the nodes in the cluster.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class NodeProgressionController implements Serializable {

    @EJB
    private DeploymentProgressFacade deploymentFacade;
    private NodeProgressionDataModel nodes;
    private NodeProgression[] selectedNode;
    private ListeningExecutorService pool =
            MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(5));

    /**
     * Creates a new instance of NodeProgressionController
     */
    public NodeProgressionController() {
    }

    @PostConstruct
    public void init() {
        loadNodes();
    }

    public NodeProgressionDataModel getNodes() {

        loadNodes();
        return nodes;
    }

    public void deleteNodes() {
        deploymentFacade.deleteAllProgress();
    }

    @Asynchronous
    public void retryNodes() {
        System.out.println("Testing retry");
        if (selectedNode != null) {
            for(NodeProgression node: selectedNode){
            System.out.println("Selected Node:" + node.toString());
            ListenableFuture<ExecResponse> future = pool.submit(
                    new RetryNodeCallable(node,
                   ScriptBuilder.builder()
                    .scriptType(ScriptBuilder.ScriptType.RECOVER)
                    .build()
                    .render(OsFamily.UNIX)));

            future.addListener(new RetryStatusTracker(future, node, deploymentFacade), pool);
            }
            selectedNode = null;
        }
    }

    public NodeProgression[] getSelectedNode() {
        return selectedNode;
    }

    public void setSelectedNode(NodeProgression[] selectedNodes) {
        this.selectedNode = selectedNodes;
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
            if (previousPhase != null && (previousPhase.equals(DeploymentPhase.INSTALL)
                    || previousPhase.equals(DeploymentPhase.CREATED))) {
                return 33;
            } else if (previousPhase != null && previousPhase.equals(DeploymentPhase.CONFIGURE)) {
                return 66;
            }
            return 0;
        }
        return 0;
    }

    private void loadNodes() {
        nodes = new NodeProgressionDataModel(deploymentFacade.findAll());
    }
}
