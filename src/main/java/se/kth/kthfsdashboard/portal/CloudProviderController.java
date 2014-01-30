package se.kth.kthfsdashboard.portal;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.RequestScoped;
import static org.jclouds.Constants.PROPERTY_CONNECTION_TIMEOUT;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.jclouds.Constants.PROPERTY_RETRY_DELAY_START;
import org.jclouds.aws.domain.Region;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_AMI_QUERY;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_CC_REGIONS;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_PORT_OPEN;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.events.StatementOnNodeCompletion;
import org.jclouds.compute.events.StatementOnNodeFailure;
import org.jclouds.compute.events.StatementOnNodeSubmission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.kthfsdashboard.provision.MessageController;
import se.kth.kthfsdashboard.provision.ProviderType;

/**
 * Controller that handles the provision of a node with the dashboard application in 
 * a baremetal machine or in a cloud machine.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@RequestScoped
public class CloudProviderController implements Serializable {

    @ManagedProperty(value = "#{messageController}")
    private MessageController messages;
    @ManagedProperty(value = "#{portalMB}")
    private PortalMB providerMB;
    @ManagedProperty(value = "#{portalCredentialsMB}")
    private PortalCredentialsMB computeCredentialsMB;
    private String provider;
    private String id;
    private String key;
    //If Openstack selected, endpoint for keystone API
    private String keystoneEndpoint;

    /**
     * Creates a new instance of CloudProviderController
     */
    public CloudProviderController() {
    }

    public PortalCredentialsMB getComputeCredentialsMB() {
        return computeCredentialsMB;
    }

    public void setComputeCredentialsMB(PortalCredentialsMB computeCredentialsMB) {
        this.computeCredentialsMB = computeCredentialsMB;
    }

    public PortalMB getProviderMB() {
        return providerMB;
    }

    public void setProviderMB(PortalMB providerMB) {
        this.providerMB = providerMB;
    }

    public MessageController getMessages() {
        return messages;
    }

    public void setMessages(MessageController messages) {
        this.messages = messages;
    }

    /*
     * Command to launch the instance
     */
    public void launchInstance() {
        // Cloud providers
        providerMB.setEnableLoginUser(!providerMB.getLoginUser().equals(""));
        providerMB.setPublicKeyEnabled(!providerMB.getPublicKey().equals(""));
        if (providerMB.isBaremetal()) {
            BaremetalProvision baremetal = new BaremetalProvision(providerMB, messages);
            baremetal.configureBaremetal();

        } else {

            CloudProvision provision = new CloudProvision(providerMB, messages);
            provision.initComputeService();
            provision.launchInstance();

        }
    }


    /*
     * Command to destroy the instances in a group
     */
    public void destroyInstance() {

        CloudProvision provision = new CloudProvision(providerMB, messages);
        provision.initComputeService();
        provision.destroyInstance(providerMB.getNodeId());

    }

    /*
     * Define the service properties for the compute service context using
     * Amazon EC2 like Query parameters and regions. Does the same for Openstack and Rackspace
     * 
     * Includes time using the ports when launching the VM instance executing the script
     */
    private Properties serviceProperties(ProviderType provider) {
        Properties properties = new Properties();
        long scriptTimeout = TimeUnit.MILLISECONDS.convert(50, TimeUnit.MINUTES);
        properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, scriptTimeout + "");
        properties.setProperty(TIMEOUT_PORT_OPEN, scriptTimeout + "");
        properties.setProperty(PROPERTY_CONNECTION_TIMEOUT, scriptTimeout + "");
        properties.setProperty(PROPERTY_MAX_RETRIES, "10");

        long scriptRetry = TimeUnit.MILLISECONDS.convert(250, TimeUnit.MILLISECONDS);
        properties.setProperty(PROPERTY_RETRY_DELAY_START, scriptRetry + "");

        switch (provider) {
            case AWS_EC2:
                properties.setProperty(PROPERTY_EC2_AMI_QUERY, "owner-id=137112412989;state=available;image-type=machine");
                properties.setProperty(PROPERTY_EC2_CC_AMI_QUERY, "");
                properties.setProperty(PROPERTY_EC2_CC_REGIONS, Region.EU_WEST_1);
                break;
            case OPENSTACK:

                break;
            case RACKSPACE:
                break;
        }

        return properties;
    }

    static enum ScriptLogger {

        INSTANCE;
        Logger logger = LoggerFactory.getLogger(CloudProviderController.class);

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
