/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
import com.google.inject.Module;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import static org.jclouds.Constants.PROPERTY_CONNECTION_TIMEOUT;
import org.jclouds.ContextBuilder;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_AMI_QUERY;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY;
import org.jclouds.chef.util.RunListBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_PORT_OPEN;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.events.StatementOnNodeCompletion;
import org.jclouds.compute.events.StatementOnNodeFailure;
import org.jclouds.compute.events.StatementOnNodeSubmission;
import org.jclouds.ec2.EC2AsyncClient;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.domain.IpProtocol;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.Ingress;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroup;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import static org.jclouds.openstack.nova.v2_0.predicates.SecurityGroupPredicates.nameEquals;
import org.jclouds.rest.RestContext;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import static org.jclouds.scriptbuilder.domain.Statements.createOrOverwriteFile;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import org.jclouds.scriptbuilder.statements.chef.InstallChefGems;
import org.jclouds.scriptbuilder.statements.git.InstallGit;
import org.jclouds.scriptbuilder.statements.ruby.InstallRubyGems;
import org.jclouds.scriptbuilder.statements.ssh.AuthorizeRSAPublicKeys;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.kthfsdashboard.virtualization.JHDFSScriptBuilder.Builder;
import se.kth.kthfsdashboard.virtualization.clusterparser.ClusterController;
import se.kth.kthfsdashboard.virtualization.clusterparser.NodeGroup;

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
    @ManagedProperty(value = "#{clusterController}")
    private ClusterController clusterController;
    private ClusterVirtualizer virtualizer;
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

    public ClusterController getClusterController() {
        return clusterController;
    }

    public void setClusterController(ClusterController clusterController) {
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
        service = initContexts();
//        virtualizer = new ClusterVirtualizer(this);
//        virtualizer.createSecurityGroups(clusterController.getCluster());
//        if(virtualizer.launchNodesBasicSetup(clusterController.getCluster())){
//            messages.addMessage("All nodes installed with basic software");
//        }
//        virtualizer.demoOnly(clusterController.getCluster());
        createSecurityGroups();
        launchNodesBasicSetup();
        demoOnly();
        messages.addSuccessMessage("Cluster launched");
        messages.clearMessages();
        //installRoles();
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
     * Define the computing cloud service you are going to use
     * Marked to deprecated, moved to another class
     */
    @Deprecated
    private ComputeService initContexts() {
        Provider check = Provider.fromString(provider);
        //We define the properties of our service
        Properties serviceDetails = serviceProperties(check);

        // example of injecting a ssh implementation
        // injecting the logging module
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule(),
                new EnterpriseConfigurationModule());

        ContextBuilder build = null;
        //We prepare the context depending of what the user selects
        switch (check) {
            case AWS_EC2:
                build = ContextBuilder.newBuilder(provider)
                        .credentials(id, key)
                        .modules(modules)
                        .overrides(serviceDetails);

                break;
            case OPENSTACK:
                build = ContextBuilder.newBuilder(provider)
                        .endpoint(keystoneEndpoint)
                        .credentials(id, key)
                        .modules(modules)
                        .overrides(serviceDetails);

                break;
            //Rachspace not implemented,
            case RACKSPACE:
                build = ContextBuilder.newBuilder(provider)
                        .credentials(id, key)
                        .modules(modules)
                        .overrides(serviceDetails);
                break;
        }

        if (build == null) {
            throw new NullPointerException("Not selected supported provider");
        }

        context = build.buildView(ComputeServiceContext.class);

        //From minecraft example, how to include your own event handlers
        context.utils().eventBus().register(ScriptLogger.INSTANCE);
        messages.addMessage("Virtualization context initialized, start opening security groups");
        return context.getComputeService();

    }

    /*
     * Define the service properties for the compute service context using
     * Amazon EC2 like Query parameters and regions. 
     * Does the same for Openstack and Rackspace but we dont setup anything for now
     * 
     * Includes time using the ports when launching the VM instance executing the script
     * 
     * Moved to another representation
     */
    @Deprecated
    private Properties serviceProperties(Provider provider) {
        Properties properties = new Properties();
        long scriptTimeout = TimeUnit.MILLISECONDS.convert(50, TimeUnit.MINUTES);
        properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, scriptTimeout + "");
        properties.setProperty(TIMEOUT_PORT_OPEN, scriptTimeout + "");
        properties.setProperty(PROPERTY_CONNECTION_TIMEOUT, scriptTimeout + "");

        switch (provider) {
            case AWS_EC2:
                properties.setProperty(PROPERTY_EC2_AMI_QUERY, "owner-id=137112412989;state=available;image-type=machine");
                properties.setProperty(PROPERTY_EC2_CC_AMI_QUERY, "");

                break;
            case OPENSTACK:
                break;
            case RACKSPACE:
                break;
        }
        return properties;
    }


    /*
     * Template of the VM we want to launch using EC2, or Openstack
     */
    @Deprecated
    private TemplateBuilder templateKTHFS(String provider, TemplateBuilder template) {
        Provider check = Provider.fromString(provider);

        switch (check) {
            case AWS_EC2:
                template.os64Bit(true);
                template.hardwareId(clusterController.getCluster().getProvider().getInstanceType());
                template.imageId(clusterController.getCluster().getProvider().getImage());
                template.locationId(clusterController.getCluster().getProvider().getRegion());
                break;
            case OPENSTACK:
                template.os64Bit(true);
                template.imageId(clusterController.getCluster().getProvider().getImage());
                template.hardwareId(clusterController.getCluster().getProvider().getRegion()
                        + "/" + clusterController.getCluster().getProvider().getInstanceType());
                break;
            case RACKSPACE:
                break;
            default:
                throw new AssertionError();
        }


        return template;
    }


    /*
     * Bootscrap Script for the nodes to launch the KTHFS Dashboard and install Chef Solo
     */
    @Deprecated
    private StatementList initBootstrapScript() {

        ImmutableList.Builder<Statement> bootstrapBuilder = ImmutableList.builder();
        bootstrapBuilder.add(exec("sudo apt-get update -qq;"));
        List<String> keys = new ArrayList();
        keys.add(publicKey);
        bootstrapBuilder.add(new AuthorizeRSAPublicKeys(keys));
        bootstrapBuilder.add(exec("sudo apt-get update -qq;"));
        bootstrapBuilder.add(exec("sudo apt-get install make;"));
        bootstrapBuilder.add(exec("sudo apt-get install -f -y -qq --force-yes ruby1.9.1-full;"));
        bootstrapBuilder.add(InstallRubyGems.builder()
                .version("1.8.10")
                .build());
        bootstrapBuilder.add(
                InstallChefGems.builder()
                .version("10.20.0").build());
        InstallGit git = new InstallGit();
        bootstrapBuilder.add(git);
        bootstrapBuilder.add(exec("apt-get install -q -y python-dev=2.7.3-0ubuntu2"));
        bootstrapBuilder.add(exec("sudo mkdir /etc/chef;"));
        bootstrapBuilder.add(exec("cd /etc/chef;"));
        bootstrapBuilder.add(exec("sudo wget http://lucan.sics.se/kthfs/solo.rb;"));
        //Setup and fetch git recipes
        bootstrapBuilder.add(exec("git config --global user.name \"Jim Dowling\";"));
        bootstrapBuilder.add(exec("git config --global user.email \"jdowling@sics.se\";"));
        bootstrapBuilder.add(exec("git config --global http.sslVerify false;"));
        bootstrapBuilder.add(exec("git config --global http.postBuffer 524288000;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));

        return new StatementList(bootstrapBuilder.build());
    }

    /*
     * Script to run Chef Solo and later restart the agents
     */
    @Deprecated
    private void runChefSolo(ImmutableList.Builder<Statement> statements) {
        statements.add(exec("sudo chef-solo -c /etc/chef/solo.rb -j /etc/chef/chef.json"));
        //statements.add(exec("sudo service kthfsagent restart"));
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

    /*
     * This method iterates over the security groups defined in the cluster file
     * It launches in parallel all the number of nodes specified in the group of the cluster file using the 
     * compute service abstraction from jclouds
     */
    @Deprecated
    private void launchNodesBasicSetup() {
        try {
            TemplateBuilder kthfsTemplate = templateKTHFS(provider, service.templateBuilder());
            //Use better Our scriptbuilder abstraction
            JHDFSScriptBuilder initScript = JHDFSScriptBuilder.builder()
                    .scriptType(JHDFSScriptBuilder.ScriptType.INIT)
                    .publicKey(key)
                    .build();

            selectProviderTemplateOptions(provider, kthfsTemplate, initScript);
            for (NodeGroup group : clusterController.getCluster().getNodes()) {
                messages.addMessage("Creating " + group.getNumber() + "  nodes in Security Group " + group.getSecurityGroup());
                Set<? extends NodeMetadata> ready = service.createNodesInGroup(group.getSecurityGroup(), group.getNumber(), kthfsTemplate.build());
                //For the demo, we keep track of the returned set of node Metadata launched and which group 
                //was
                messages.addMessage("Nodes created in Security Group " + group.getSecurityGroup() + " with "
                        + "basic setup");
                nodes.put(group.getSecurityGroup(), ready);

                //Ignore for now, this is for future does not affect for the demo
                //Fetch the nodes info so we can launch first mgm before the rest!
                Set<String> roles = new HashSet(group.getRoles());
                if (roles.contains("MySQLCluster*mgm")) {
                    Iterator<? extends NodeMetadata> iter = ready.iterator();
                    while (iter.hasNext()) {
                        //Add private ip to mgm
                        NodeMetadata node = iter.next();
                        mgm.addAll(node.getPrivateAddresses());
                        //need to also check if they have ndb
                        if (roles.contains("MySQLCluster*ndb")) {
                            ndbs.addAll(node.getPrivateAddresses());
                        }
                        first.put(node, group.getRoles());
                    }
                    continue;
                } else {
                    Iterator<? extends NodeMetadata> iter = ready.iterator();
                    while (iter.hasNext()) {
                        NodeMetadata node = iter.next();
                        if (roles.contains("MySQLCluster*ndb")) {
                            ndbs.addAll(node.getPrivateAddresses());
                        }
                        if (roles.contains("MySQLCluster*memcached")) {
                            mySQLClients.addAll(node.getPrivateAddresses());
                        }
                        rest.put(node, group.getRoles());
                    }
                }

            }
        } catch (RunNodesException e) {
            System.out.println("error adding nodes to group "
                    + "ups something got wrong on the nodes");
        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
        }
    }

    /*
     * Private Method which creates the securitygroups for the cluster 
     * through the rest client implementations in jclouds.
     */
    @Deprecated
    private void createSecurityGroups() {
        //Data structures which contains all the mappings of the ports that the roles need to be opened
        RoleMapPorts commonTCP = new RoleMapPorts(RoleMapPorts.PortType.COMMON);
        RoleMapPorts portsTCP = new RoleMapPorts(RoleMapPorts.PortType.TCP);
        RoleMapPorts portsUDP = new RoleMapPorts(RoleMapPorts.PortType.UDP);

        String region = clusterController.getCluster().getProvider().getRegion();
        //List to gather  ports, we initialize with the ports defined by the user
        List<Integer> globalPorts = new LinkedList<Integer>(clusterController.getCluster().getAuthorizeSpecificPorts());

        //All need the kthfsagent ports opened
        globalPorts.addAll(Ints.asList(commonTCP.get("kthfsagent")));
        //For each basic role, we map the ports in that role into a list which we append to the commonPorts
        for (String commonRole : clusterController.getCluster().getAuthorizePorts()) {
            if (commonTCP.containsKey(commonRole)) {
                //Use guava library to transform the array into a list, add all the ports
                List<Integer> portsRole = Ints.asList(commonTCP.get(commonRole));
                globalPorts.addAll(portsRole);
            }
        }


        //If EC2 client
        if (provider.equals(Provider.AWS_EC2.toString())) {
            //Unwrap the compute service context and retrieve a rest context to speak with EC2
            RestContext<EC2Client, EC2AsyncClient> temp = context.unwrap();
            //Fetch a synchronous rest client
            EC2Client client = temp.getApi();
            //For each group of the security groups
            for (NodeGroup group : clusterController.getCluster().getNodes()) {
                String groupName = "jclouds#" + group.getSecurityGroup();// jclouds way of defining groups
                Set<Integer> openTCP = new HashSet<Integer>(); //To avoid opening duplicate ports
                Set<Integer> openUDP = new HashSet<Integer>();// gives exception upon trying to open duplicate ports in a group
                System.out.printf("%d: creating security group: %s%n", System.currentTimeMillis(),
                        group.getSecurityGroup());
                //create security group
                messages.addMessage("Creating Security Group: " + group.getSecurityGroup());
                try {
                    client.getSecurityGroupServices().createSecurityGroupInRegion(
                            region, groupName, group.getSecurityGroup());
                } catch (Exception e) {

                    //If group already exists continue to the next group
                    continue;
                }
                //Open the ports for that group
                for (String authPort : group.getAuthorizePorts()) {

                    //Authorize the ports for TCP and UDP roles in cluster file for that group

                    if (portsTCP.containsKey(authPort)) {
                        for (int port : portsTCP.get(authPort)) {
                            if (!openTCP.contains(port)) {
                                client.getSecurityGroupServices().authorizeSecurityGroupIngressInRegion(region,
                                        groupName, IpProtocol.TCP, port, port, "0.0.0.0/0");
                                openTCP.add(port);
                            }
                        }

                        for (int port : portsUDP.get(authPort)) {
                            if (!openUDP.contains(port)) {
                                client.getSecurityGroupServices().authorizeSecurityGroupIngressInRegion(region,
                                        groupName, IpProtocol.UDP, port, port, "0.0.0.0/0");
                                openUDP.add(port);
                            }
                        }
                    }
                }
                //Authorize the global ports TCP
                for (int port : Ints.toArray(globalPorts)) {
                    if (!openTCP.contains(port)) {
                        client.getSecurityGroupServices().authorizeSecurityGroupIngressInRegion(region,
                                groupName, IpProtocol.TCP, port, port, "0.0.0.0/0");
                        openTCP.add(port);
                    }
                }
                //This is a delay we must use for EC2. There is a limit on REST requests and if we dont limit the
                //bursts of the requests it will fail
                try {
                    Thread.sleep(15000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //If openstack nova2 client
        //Similar structure to EC2 but changes apis
        if (provider.equals(Provider.OPENSTACK.toString())) {
            RestContext<NovaApi, NovaAsyncApi> temp = context.unwrap();
            //+++++++++++++++++
            //This stuff below is weird, founded in a code snippet in a workshop on jclouds. Still it works
            //Code not from documentation
            Optional<? extends SecurityGroupApi> securityGroupExt = temp.getApi().getSecurityGroupExtensionForZone(region);
            System.out.println("  Security Group Support: " + securityGroupExt.isPresent());
            if (securityGroupExt.isPresent()) {
                SecurityGroupApi client = securityGroupExt.get();
                //+++++++++++++++++    
                //For each group of the security groups
                for (NodeGroup group : clusterController.getCluster().getNodes()) {
                    String groupName = "jclouds-" + group.getSecurityGroup(); //jclouds way of defining groups
                    Set<Integer> openTCP = new HashSet<Integer>(); //To avoid opening duplicate ports
                    Set<Integer> openUDP = new HashSet<Integer>();// gives exception upon trying to open duplicate ports in a group
                    System.out.printf("%d: creating security group: %s%n", System.currentTimeMillis(),
                            group.getSecurityGroup());
                    //create security group
                    if (!client.list().anyMatch(nameEquals(groupName))) {
                        messages.addMessage("Creating security group: " + group.getSecurityGroup());
                        SecurityGroup created = client.createWithDescription(groupName, group.getSecurityGroup());
                        //get the ports
                        for (String authPort : group.getAuthorizePorts()) {
                            //Authorize the ports for TCP and UDP
                            if (portsTCP.containsKey(authPort)) {
                                for (int port : portsTCP.get(authPort)) {
                                    if (!openTCP.contains(port)) {
                                        Ingress ingress = Ingress.builder()
                                                .fromPort(port)
                                                .toPort(port)
                                                .ipProtocol(org.jclouds.openstack.nova.v2_0.domain.IpProtocol.TCP)
                                                .build();
                                        client.createRuleAllowingCidrBlock(created.getId(), ingress, "0.0.0.0/0");
                                        openTCP.add(port);
                                    }

                                }
                                for (int port : portsUDP.get(authPort)) {
                                    if (!openUDP.contains(port)) {
                                        Ingress ingress = Ingress.builder()
                                                .fromPort(port)
                                                .toPort(port)
                                                .ipProtocol(org.jclouds.openstack.nova.v2_0.domain.IpProtocol.UDP)
                                                .build();
                                        client.createRuleAllowingCidrBlock(created.getId(), ingress, "0.0.0.0/0");
                                        openUDP.add(port);
                                    }

                                }
                            }

                        }
                        //Authorize the global ports
                        for (int port : Ints.toArray(globalPorts)) {
                            if (!openTCP.contains(port)) {
                                Ingress ingress = Ingress.builder()
                                        .fromPort(port)
                                        .toPort(port)
                                        .ipProtocol(org.jclouds.openstack.nova.v2_0.domain.IpProtocol.TCP)
                                        .build();
                                client.createRuleAllowingCidrBlock(created.getId(), ingress, "0.0.0.0/0");
                                openTCP.add(port);
                            }
                        }
                    }
                }
            }
        }
    }

    /*
     * This is the code for the demo
     *
     */
    @Deprecated
    private void demoOnly() {
        messages.addMessage("Starting specific configuration of the nodes...");
        //Fetch all the addresses we need.
        List<String> ndbsDemo = new LinkedList();
        List<String> mgmDemo = new LinkedList();
        List<String> mysqlDemo = new LinkedList();
        List<String> datanodesDemo = new LinkedList();
        List<String> namenodesDemo = new LinkedList();

        //Fetch the predefined group nodes in the demo cluster file
        //I.E: ndb, mgm, mysql, datanodes,namenodes
        //Once we find our group, get the node metadatas from our hashmap store and fetch their private IPs
        //Add to the corresponding list
        if (nodes.containsKey("ndb")) {
            for (NodeMetadata node : nodes.get("ndb")) {
                List<String> privateIPs = new LinkedList(node.getPrivateAddresses());
                ndbsDemo.add(privateIPs.get(0));
            }
        }
        if (nodes.containsKey("mgm")) {
            for (NodeMetadata node : nodes.get("mgm")) {
                List<String> privateIPs = new LinkedList(node.getPrivateAddresses());
                mgmDemo.add(privateIPs.get(0));
            }
        }
        if (nodes.containsKey("mysql")) {
            for (NodeMetadata node : nodes.get("mysql")) {
                List<String> privateIPs = new LinkedList(node.getPrivateAddresses());
                mysqlDemo.add(privateIPs.get(0));
            }
        }
        if (nodes.containsKey("namenodes")) {
            for (NodeMetadata node : nodes.get("namenodes")) {
                List<String> privateIPs = new LinkedList(node.getPrivateAddresses());
                namenodesDemo.add(privateIPs.get(0));
            }
        }
        if (nodes.containsKey("datanodes")) {
            for (NodeMetadata node : nodes.get("datanodes")) {
                List<String> privateIPs = new LinkedList(node.getPrivateAddresses());
                datanodesDemo.add(privateIPs.get(0));
            }
        }

        //Start the setup of the nodes
        //First start with the MGM
        //Shit code follows, done fast not proud of it, repeated as hell

        Builder jhdfsBuilder = JHDFSScriptBuilder.builder();
        //All follow the same idea, we go over the security groups and fetch
        messages.addMessage("Starting the MySQL Cluster....");
        for (NodeGroup group : clusterController.getCluster().getNodes()) {
            if (group.getSecurityGroup().equals("mgm")) {
                messages.addMessage("Starting setup of mgm Node..");
                //The nodes for that group from our hashmap store
                Set<? extends NodeMetadata> elements = nodes.get(group.getSecurityGroup());
                Iterator<? extends NodeMetadata> iter = elements.iterator();
                //Iterate over
                while (iter.hasNext()) {
                    NodeMetadata data = iter.next();
                    List<String> ips = new LinkedList<String>(data.getPrivateAddresses());
                    
                    
                    //Prepare the script
                    //ImmutableList.Builder<Statement> roleBuilder = ImmutableList.builder();
                    //We build the json chef file
                    messages.addMessage("Generating JSON chef file for node...");
                    //createNodeConfiguration(roleBuilder, ndbsDemo, mgmDemo, mysqlDemo, namenodesDemo, data, group);
                    //Append at the end of the script the need to launch the chef command and run the restart command
                    //runChefSolo(roleBuilder);
                    StatementList nodeConfig = new StatementList(
                            jhdfsBuilder.scriptType(JHDFSScriptBuilder.ScriptType.JHDFS)
                            .ndbs(ndbsDemo)
                            .mgms(mgmDemo)
                            .mysql(mysqlDemo)
                            .namenodes(namenodesDemo)
                            .roles(group.getRoles())
                            .nodeIP(ips.get(0))
                            .build());
                            
                            //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }

        //All follow the same description
        for (NodeGroup group : clusterController.getCluster().getNodes()) {
            if (group.getSecurityGroup().equals("ndb")) {
                messages.addMessage("Starting setup of ndb Node...");
                Set<? extends NodeMetadata> elements = nodes.get(group.getSecurityGroup());
                Iterator<? extends NodeMetadata> iter = elements.iterator();
                while (iter.hasNext()) {
                    NodeMetadata data = iter.next();
                    List<String> ips = new LinkedList<String>(data.getPrivateAddresses());
                    
                    
                    //Prepare the script
                    //ImmutableList.Builder<Statement> roleBuilder = ImmutableList.builder();
                    //We build the json chef file
                    messages.addMessage("Generating JSON chef file for node...");
                    //createNodeConfiguration(roleBuilder, ndbsDemo, mgmDemo, mysqlDemo, namenodesDemo, data, group);
                    //Append at the end of the script the need to launch the chef command and run the restart command
                    //runChefSolo(roleBuilder);
                     StatementList nodeConfig = new StatementList(
                            jhdfsBuilder.scriptType(JHDFSScriptBuilder.ScriptType.JHDFS)
                            .ndbs(ndbsDemo)
                            .mgms(mgmDemo)
                            .mysql(mysqlDemo)
                            .namenodes(namenodesDemo)
                            .roles(group.getRoles())
                            .nodeIP(ips.get(0))
                            .build());
                            
                            //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }

        for (NodeGroup group : clusterController.getCluster().getNodes()) {
            if (group.getSecurityGroup().equals("mysql")) {
                messages.addMessage("Starting setup of MySQLd Node..");
                Set<? extends NodeMetadata> elements = nodes.get(group.getSecurityGroup());
                Iterator<? extends NodeMetadata> iter = elements.iterator();
                while (iter.hasNext()) {
                    NodeMetadata data = iter.next();
                    List<String> ips = new LinkedList<String>(data.getPrivateAddresses());
                    
                    
                    //Prepare the script
                    //ImmutableList.Builder<Statement> roleBuilder = ImmutableList.builder();
                    //We build the json chef file
                    messages.addMessage("Generating JSON chef file for node...");
                    //createNodeConfiguration(roleBuilder, ndbsDemo, mgmDemo, mysqlDemo, namenodesDemo, data, group);
                    //Append at the end of the script the need to launch the chef command and run the restart command
                    //runChefSolo(roleBuilder);
                     StatementList nodeConfig = new StatementList(
                            jhdfsBuilder.scriptType(JHDFSScriptBuilder.ScriptType.JHDFS)
                            .ndbs(ndbsDemo)
                            .mgms(mgmDemo)
                            .mysql(mysqlDemo)
                            .namenodes(namenodesDemo)
                            .roles(group.getRoles())
                            .nodeIP(ips.get(0))
                            .build());
                            
                            //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }

        messages.addMessage("Starting Hadoop nodes...");
        for (NodeGroup group : clusterController.getCluster().getNodes()) {
            if (group.getSecurityGroup().equals("namenodes")) {
                messages.addMessage("Starting setup of Namenode");
                Set<? extends NodeMetadata> elements = nodes.get(group.getSecurityGroup());
                Iterator<? extends NodeMetadata> iter = elements.iterator();
                while (iter.hasNext()) {
                    NodeMetadata data = iter.next();
                    List<String> ips = new LinkedList<String>(data.getPrivateAddresses());
                    
                    
                    //Prepare the script
                    //ImmutableList.Builder<Statement> roleBuilder = ImmutableList.builder();
                    //We build the json chef file
                    messages.addMessage("Generating JSON chef file for node...");
                    //createNodeConfiguration(roleBuilder, ndbsDemo, mgmDemo, mysqlDemo, namenodesDemo, data, group);
                    //Append at the end of the script the need to launch the chef command and run the restart command
                    //runChefSolo(roleBuilder);
                     StatementList nodeConfig = new StatementList(
                            jhdfsBuilder.scriptType(JHDFSScriptBuilder.ScriptType.JHDFS)
                            .ndbs(ndbsDemo)
                            .mgms(mgmDemo)
                            .mysql(mysqlDemo)
                            .namenodes(namenodesDemo)
                            .roles(group.getRoles())
                            .nodeIP(ips.get(0))
                            .build());
                            
                            //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }
        for (NodeGroup group : clusterController.getCluster().getNodes()) {
            if (group.getSecurityGroup().equals("datanodes")) {
                messages.addMessage("Starting setup of Datanode");
                Set<? extends NodeMetadata> elements = nodes.get(group.getSecurityGroup());
                Iterator<? extends NodeMetadata> iter = elements.iterator();
                while (iter.hasNext()) {
                    NodeMetadata data = iter.next();
                    List<String> ips = new LinkedList<String>(data.getPrivateAddresses());
                    
                    
                    //Prepare the script
                    //ImmutableList.Builder<Statement> roleBuilder = ImmutableList.builder();
                    //We build the json chef file
                    messages.addMessage("Generating JSON chef file for node...");
                    //createNodeConfiguration(roleBuilder, ndbsDemo, mgmDemo, mysqlDemo, namenodesDemo, data, group);
                    //Append at the end of the script the need to launch the chef command and run the restart command
                    //runChefSolo(roleBuilder);
                    StatementList nodeConfig = new StatementList(
                            jhdfsBuilder.scriptType(JHDFSScriptBuilder.ScriptType.JHDFS)
                            .ndbs(ndbsDemo)
                            .mgms(mgmDemo)
                            .mysql(mysqlDemo)
                            .namenodes(namenodesDemo)
                            .roles(group.getRoles())
                            .nodeIP(ips.get(0))
                            .build());
                            
                            //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }
    }

    //For Demo only 
    /*
     * Here we generate the json file and the runlists we need for chef in the nodes
     * We need the ndbs, mgms, mysqlds and namenodes ips.
     * Also we need to know the security group to generate the runlist of recipes for that group based on 
     * the roles and the node metadata to get its ips.
     */
    @Deprecated
    private void createNodeConfiguration(ImmutableList.Builder<Statement> statements,
            List<String> ndbs, List<String> mgm, List<String> mysql, List<String> namenodes,
            NodeMetadata data, NodeGroup group) {

        //First we generate the recipe runlist based on the roles defined in the security group of the cluster
        List<String> runlist = createRunList(group);
        //Start json
        StringBuilder json = new StringBuilder();
        //Open json bracket
        json.append("{");
        //First generate the ndb fragment
        // JIM: Note there can be multiple mgm servers, not just one.
        json.append("\"ndb\":{  \"mgm_server\":{\"addrs\": [");

        //Iterate mgm servers and add them.

        for (int i = 0; i < mgm.size(); i++) {
            if (i == mgm.size() - 1) {
                json.append("\"").append(mgm.get(i)).append("\"");
            } else {
                json.append("\"").append(mgm.get(i)).append("\",");
            }
        }
        json.append("]},");
        //Iterate ndbds addresses
        json.append("\"ndbd\":{\"addrs\":[");
        for (int i = 0; i < ndbs.size(); i++) {
            if (i == ndbs.size() - 1) {
                json.append("\"").append(ndbs.get(i)).append("\"");
            } else {
                json.append("\"").append(ndbs.get(i)).append("\",");
            }
        }
        json.append("]},");
        //Get the mgms ips and add to the end the ips of the mysqlds
        List<String> ndapi = new LinkedList(mgm);
        ndapi.addAll(mysql);
        //Generate ndbapi with ndapi ips
        json.append("\"ndbapi\":{\"addrs\":[");
        for (int i = 0; i < ndapi.size(); i++) {
            if (i == ndapi.size() - 1) {
                json.append("\"").append(ndapi.get(i)).append("\"");
            } else {
                json.append("\"").append(ndapi.get(i)).append("\",");
            }
        }
        json.append("]},");
        //Get the nodes private ip
        List<String> ips = new LinkedList(data.getPrivateAddresses());
        //add the ip in the json
        json.append("\"ip\":\"").append(ips.get(0)).append("\",");
        //***
        json.append("\"data_memory\":\"120\",");
        json.append("\"num_ndb_slots_per_client\":\"2\"},");
        json.append("\"memcached\":{\"mem_size\":\"128\"},");
        //***
        //Generate collectd fragment
        json.append("\"collectd\":{\"server\":\"").append(privateIP).append("\",");
        json.append("\"clients\":[");
        //Depending of the security group name of the demo we specify which collectd config to use
        if (group.getSecurityGroup().equals("mysql") // JIM: We can just have an empty clients list for mgm and ndb nodes    
                //                || group.getSecurityGroup().equals("mgm")
                //                || group.getSecurityGroup().equals("ndb")
                ) {
            json.append("\"mysql\"");
        }
        if (group.getSecurityGroup().equals("datanodes")) {
            json.append("\"dn\"");
        }
        if (group.getSecurityGroup().equals("namenodes")) {
            json.append("\"nn\"");
        }
        json.append("]},");
        //Generate kthfs fragment
        //server ip of the dashboard
        json.append("\"kthfs\":{\"server_ip\":\"").append(privateIP).append("\",");
        //mgm ip
        json.append("\"ndb_connectstring\":\"").append(mgm.get(0)).append("\",");
        //namenodes ips
        json.append("\"namenode\":{\"addrs\":[");

        for (int i = 0; i < namenodes.size(); i++) {
            if (i == namenodes.size() - 1) {
                json.append("\"").append(namenodes.get(i)).append("\"");
            } else {
                json.append("\"").append(namenodes.get(i)).append("\",");
            }
        }
        json.append("]},");
        //My own ip
        json.append("\"ip\":\"").append(ips.get(0)).append("\"");
        json.append("},");
        //Recipe runlist append in the json
        json.append("\"run_list\":[");
        for (int i = 0; i < runlist.size(); i++) {
            if (i == runlist.size() - 1) {
                json.append("\"").append(runlist.get(i)).append("\"");
            } else {
                json.append("\"").append(runlist.get(i)).append("\",");
            }
        }
        //close the json
        json.append("]}");
        //Create the file in this directory in the node
        statements.add(createOrOverwriteFile("/etc/chef/chef.json", ImmutableSet.of(json.toString())));

    }

    /*
     * We select the recipes by inspecting the roles defined in the security group in the cluster file
     */
    @Deprecated
    private List<String> createRunList(NodeGroup group) {
        RunListBuilder builder = new RunListBuilder();
        builder.addRecipe("kthfsagent");

        boolean collectdAdded = false;
        //Look at the roles, if it matches add the recipes for that role
        for (String role : group.getRoles()) {
            if (role.equals("MySQLCluster*ndb")) {

                builder.addRecipe("ndb::ndbd");
                builder.addRecipe("ndb::ndbd-kthfs");
                collectdAdded = true;
            }
            if (role.equals("MySQLCluster*mysqld")) {

                builder.addRecipe("ndb::mysqld");
                builder.addRecipe("ndb::mysqld-kthfs");
                collectdAdded = true;
            }
            if (role.equals("MySQLCluster*mgm")) {

                builder.addRecipe("ndb::mgmd");

                builder.addRecipe("ndb::mgmd-kthfs");
                collectdAdded = true;
            }
            if (role.equals("MySQLCluster*memcached")) {
                builder.addRecipe("ndb::memcached");
                builder.addRecipe("ndb::memcached-kthfs");
            }

            //This are for the Hadoop nodes
            if (role.equals("KTHFS*namenode")) {
                builder.addRecipe("java");
                builder.addRecipe("kthfs::namenode");
                collectdAdded = true;
            }
            if (role.equals("KTHFS*datanode")) {
                builder.addRecipe("java");
                builder.addRecipe("kthfs::datanode");
                collectdAdded = true;
            }
            if (collectdAdded) {
                builder.addRecipe("collect::attr-driven");
            }
            // We always need to restart the kthfsagent after we have
            // updated its list of services
            builder.addRecipe("java::openjdk");
            builder.addRecipe("kthfsagent::restart");

        }

        return builder.build();


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
