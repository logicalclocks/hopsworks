/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.events.StatementOnNodeCompletion;
import org.jclouds.compute.events.StatementOnNodeFailure;
import org.jclouds.compute.events.StatementOnNodeSubmission;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.kthfsdashboard.virtualization.clusterparser.ClusterManagementController;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class VirtualizationController implements Serializable {

    @ManagedProperty(value = "#{messageController}")
    private MessageController messages;
    @ManagedProperty(value = "#{computeCredentialsMB}")
    private ComputeCredentialsMB computeCredentialsMB;
    @ManagedProperty(value = "#{clusterManagementController}")
    private ClusterManagementController clusterController;
    private ClusterProvision virtualizer;
    private String provider;
    private String id;
    private String key;
    private String privateIP;
    private String publicKey;
    //If Openstack selected, endpoint for keystone API
    private String keystoneEndpoint;
    private ComputeService service;
    private ComputeServiceContext context;
    private Map<String, Set<? extends NodeMetadata>> nodes = new HashMap();
    private Map<NodeMetadata, List<String>> first = new HashMap();
    private Map<NodeMetadata, List<String>> rest = new HashMap();
    private List<String> ndbs = new LinkedList();
    private List<String> mgm = new LinkedList();
    private List<String> mySQLClients = new LinkedList();

    /**
     * Creates a new instance of VirtualizationController
     */
    public VirtualizationController() {
    }

    public ComputeCredentialsMB getComputeCredentialsMB() {
        return computeCredentialsMB;
    }

    public void setComputeCredentialsMB(ComputeCredentialsMB computeCredentialsMB) {
        this.computeCredentialsMB = computeCredentialsMB;
    }

    public ClusterManagementController getClusterController() {
        return clusterController;
    }

    public void setClusterController(ClusterManagementController clusterController) {
        this.clusterController = clusterController;
    }

    public MessageController getMessages() {
        return messages;
    }

    public void setMessages(MessageController messages) {
        this.messages = messages;
    }

    public String getProvider() {
        return provider;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getPrivateIP() {
        return privateIP;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getKeystoneEndpoint() {
        return keystoneEndpoint;
    }
    

    /*
     * Command to launch the instance
     */
    public void launchCluster() {
        setCredentials();
        messages.addMessage("Setting up credentials and initializing virtualization context...");
        //service = initContexts();
        virtualizer = new ClusterProvision(this);
        virtualizer.createSecurityGroups(clusterController.getCluster());
        if(virtualizer.launchNodesBasicSetup(clusterController.getCluster())){
//        if(virtualizer.parallelLaunchNodesBasicSetup(clusterController.getCluster())){
            messages.addMessage("All nodes launched");
            virtualizer.installPhase();
            virtualizer.deployingConfigurations(clusterController.getCluster());
            messages.addSuccessMessage("Cluster launched");
        }
        else{
            messages.addSuccessMessage("Deployment failure");
        }
        
        messages.clearMessages();

    }

    /*
     * Private methods used by the controller
     */
    /*
     * Set the credentials chosen by the user to launch the instance
     * retrieves the information from the credentials page
     */
    private void setCredentials() {
        Provider check = Provider.fromString(clusterController.getCluster().getProvider().getName());
        if (!computeCredentialsMB.isAwsec2()
                && Provider.AWS_EC2
                .equals(check)) {
            provider = Provider.AWS_EC2.toString();
            id = computeCredentialsMB.getAwsec2Id();
            key = computeCredentialsMB.getAwsec2Key();
        }

        if (!computeCredentialsMB.isOpenstack()
                && Provider.OPENSTACK
                .equals(check)) {
            provider = Provider.OPENSTACK.toString();
            id = computeCredentialsMB.getOpenstackId();
            key = computeCredentialsMB.getOpenstackKey();
            keystoneEndpoint = computeCredentialsMB.getOpenstackKeystone();
        }
        
        //Setup the private IP for the nodes to know where is the dashboard
        //Add public key for managing nodes using ssh.
        privateIP = computeCredentialsMB.getPrivateIP();
        publicKey = computeCredentialsMB.getPublicKey();

    }

    /*
     * Select extra options depending of the provider we selected
     * For example we include the bootstrap script to download and do basic setup the first time
     * For openstack we override the need to generate a key pair and the user used by the image to login
     * EC2 jclouds detects the login by default
     */
    private void selectProviderTemplateOptions(String provider, TemplateBuilder kthfsTemplate, JHDFSScriptBuilder script) {
        Provider check = Provider.fromString(provider);
        StatementList bootstrap = new StatementList(script);
        switch (check) {
            case AWS_EC2:
                kthfsTemplate.options(EC2TemplateOptions.Builder
                        .runScript(bootstrap));
                break;
            case OPENSTACK:
                kthfsTemplate.options(NovaTemplateOptions.Builder
                        .overrideLoginUser(clusterController.getCluster().getProvider().getLoginUser())
                        .generateKeyPair(true)
                        .runScript(bootstrap));
                break;
            case RACKSPACE:

                break;
            default:
                throw new AssertionError();
        }
    }

    static enum ScriptLogger {

        INSTANCE;
        Logger logger = LoggerFactory.getLogger(VirtualizationController.class);

        @Subscribe
        @AllowConcurrentEvents
        public void onStart(StatementOnNodeSubmission event) {
            logger.info(">> running {} on node({})", event.getStatement(), event.getNode().getId());
            if (logger.isDebugEnabled()) {
                logger.debug(">> script for {} on node({})\n{}", new Object[]{event.getStatement(), event.getNode().getId(),
                            event.getStatement().render(org.jclouds.scriptbuilder.domain.OsFamily.UNIX)});
            }
        }

        @Subscribe
        @AllowConcurrentEvents
        public void onFailure(StatementOnNodeFailure event) {
            logger.error("<< error running {} on node({}): {}", new Object[]{event.getStatement(), event.getNode().getId(),
                        event.getCause().getMessage()}, event.getCause());
        }

        @Subscribe
        @AllowConcurrentEvents
        public void onSuccess(StatementOnNodeCompletion event) {
            ExecResponse arg0 = event.getResponse();
            if (arg0.getExitStatus() != 0) {
                logger.error("<< error running {} on node({}): {}", new Object[]{event.getStatement(), event.getNode().getId(),
                            arg0});
            } else {
                logger.info("<< success executing {} on node({}): {}", new Object[]{event.getStatement(),
                            event.getNode().getId(), arg0});
            }
        }
    }
}
