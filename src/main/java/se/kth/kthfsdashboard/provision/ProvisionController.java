package se.kth.kthfsdashboard.provision;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import java.io.Serializable;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.events.StatementOnNodeCompletion;
import org.jclouds.compute.events.StatementOnNodeFailure;
import org.jclouds.compute.events.StatementOnNodeSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.kthfsdashboard.baremetal.BaremetalClusterProvision;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.virtualization.VirtualizedClusterProvision;
import se.kth.kthfsdashboard.virtualization.clusterparser.Baremetal;
import se.kth.kthfsdashboard.virtualization.clusterparser.Cluster;
import se.kth.kthfsdashboard.virtualization.clusterparser.ClusterManagementController;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@Stateless
@ManagedBean
@SessionScoped
public class ProvisionController implements Serializable {

    @EJB
    private DeploymentProgressFacade deploymentFacade;
    @EJB
    private HostEJB hostEJB;
    @EJB
    private CredentialsFacade credentialsEJB;
    @ManagedProperty(value = "#{messageController}")
    private MessageController messages;
    @ManagedProperty(value = "#{clusterManagementController}")
    private ClusterManagementController clusterController;
    private Provision provisioner;
    private String provider;
    private String id;
    private String key;
    private String privateIP;
    private String publicKey;
    private String privateKey;
    private boolean isBaremetal = false;
    //If Openstack selected, endpoint for keystone API
    private String keystoneEndpoint;
    private ComputeService service;
    private ComputeServiceContext context;

    /**
     * Creates a new instance of ProvisionController
     */
    public ProvisionController() {
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

    public DeploymentProgressFacade getDeploymentProgressFacade() {
        return deploymentFacade;
    }

    public HostEJB getHostEJB() {
        return hostEJB;
    }

    public Cluster getCluster() {
        return clusterController.getCluster();
    }

    public Baremetal getBaremetalCluster() {
        return clusterController.getBaremetalCluster();
    }

    public String getPrivateKey() {
        return privateKey;
    }

    /*
     * Command to launch the instance
     */
    public void launchCluster() {
        setCredentials();
        launchProvisioner();

    }

    @Asynchronous
    public void launchProvisioner() {
        boolean installPhase;
        if (isBaremetal) {
            provisioner = new BaremetalClusterProvision(this);
            installPhase = clusterController.getBaremetalCluster().isInstallPhase();

        } else {
            provisioner = new VirtualizedClusterProvision(this);
            installPhase = clusterController.getCluster().isInstallPhase();
        }

        provisioner.initializeCluster();
        if (provisioner.launchNodesBasicSetup()) {

            if (installPhase) {
                provisioner.installPhase();
            }
            provisioner.deployingConfigurations();

        }


    }

    /*
     * Private methods used by the controller
     */
    /*
     * Set the credentials chosen by the user to launch the instance
     * retrieves the information from the credentials page
     */
    private void setCredentials() {

        PaasCredentials credentials = credentialsEJB.find();
        ProviderType check = ProviderType.fromString(credentials.getProvider());
        if (ProviderType.AWS_EC2.equals(check)) {
            provider = ProviderType.AWS_EC2.toString();

            id = credentials.getAccountId();
            key = credentials.getAccessKey();
        } else if (ProviderType.OPENSTACK.equals(check)) {
            provider = ProviderType.OPENSTACK.toString();
            id = credentials.getAccountId();
            key = credentials.getAccessKey();
            keystoneEndpoint = credentials.getKeystoneURL();
        } else if (ProviderType.BAREMETAL.equals(check)) {
//            privateKey = credentials.getPrivateKey();
            isBaremetal=true;
        }

        //Setup the private IP for the nodes to know where is the dashboard
        //Add public key for managing nodes using ssh.
        privateIP = credentials.getDashboardIP();
        publicKey = credentials.getPublicKey();

    }

    public static enum ScriptLogger {

        INSTANCE;
        Logger logger = LoggerFactory.getLogger(ProvisionController.class);

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
