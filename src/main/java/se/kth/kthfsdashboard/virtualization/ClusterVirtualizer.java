/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import com.google.inject.Module;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static org.jclouds.Constants.PROPERTY_CONNECTION_TIMEOUT;
import org.jclouds.ContextBuilder;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_AMI_QUERY;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_PORT_OPEN;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.domain.IpProtocol;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.Ingress;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroup;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import static org.jclouds.openstack.nova.v2_0.predicates.SecurityGroupPredicates.nameEquals;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.jclouds.sshj.config.SshjSshClientModule;
import se.kth.kthfsdashboard.virtualization.clusterparser.Cluster;
import se.kth.kthfsdashboard.virtualization.clusterparser.NodeGroup;

/**
 * Representation of a Cluster Virtualization process
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public final class ClusterVirtualizer {

    private ComputeService service;
    private Provider provider;
    private String id;
    private String key;
    private String endpoint;
    private String publicKey;
    private String privateIP;
    private MessageController messages;
    private Map<String, Set<? extends NodeMetadata>> nodes = new HashMap();
    private Map<NodeMetadata, List<String>> mgms = new HashMap();
    private Map<NodeMetadata, List<String>> ndbs= new HashMap();
    private Map<NodeMetadata, List<String>> mysqlds = new HashMap();
    private Map<NodeMetadata, List<String>> namenodes= new HashMap();
    private List<String> ndbsIP = new LinkedList();
    private List<String> mgmIP = new LinkedList();
    private List<String> mySQLClientsIP = new LinkedList();
    private List<String> namenodesIP = new LinkedList();

    /*
     * Constructor of a ClusterVirtualizer
     */
    public ClusterVirtualizer(VirtualizationController controller) {
        this.provider = Provider.fromString(controller.getProvider());
        this.id = controller.getId();
        this.key = controller.getKey();
        this.endpoint = controller.getKeystoneEndpoint();
        this.privateIP = controller.getPrivateIP();
        this.publicKey = controller.getPublicKey();
        this.messages = controller.getMessages();
        this.service = initContext();
    }

    /*
     * Method which creates the securitygroups for the cluster 
     * through the rest client implementations in jclouds.
     */
    public void createSecurityGroups(Cluster cluster) {
        //Data structures which contains all the mappings of the ports that the roles need to be opened
        RoleMapPorts commonTCP = new RoleMapPorts(RoleMapPorts.PortType.COMMON);
        RoleMapPorts portsTCP = new RoleMapPorts(RoleMapPorts.PortType.TCP);
        RoleMapPorts portsUDP = new RoleMapPorts(RoleMapPorts.PortType.UDP);

        String region = cluster.getProvider().getRegion();
        //List to gather  ports, we initialize with the ports defined by the user
        List<Integer> globalPorts = new LinkedList<Integer>(cluster.getAuthorizeSpecificPorts());

        //All need the kthfsagent ports opened
        globalPorts.addAll(Ints.asList(commonTCP.get("kthfsagent")));
        //For each basic role, we map the ports in that role into a list which we append to the commonPorts
        for (String commonRole : cluster.getAuthorizePorts()) {
            if (commonTCP.containsKey(commonRole)) {
                //Use guava library to transform the array into a list, add all the ports
                List<Integer> portsRole = Ints.asList(commonTCP.get(commonRole));
                globalPorts.addAll(portsRole);
            }
        }


        //If EC2 client
        if (provider.toString().equals(Provider.AWS_EC2.toString())) {
            //Unwrap the compute service context and retrieve a rest context to speak with EC2
            EC2Client client = service.getContext().unwrap();
            //Fetch a synchronous rest client
//            EC2Client client = temp.getApi();
            //For each group of the security groups
            for (NodeGroup group : cluster.getNodes()) {
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
        if (provider.toString().equals(Provider.OPENSTACK.toString())) {
            NovaApi temp = service.getContext().unwrap();
            //+++++++++++++++++
            //This stuff below is weird, founded in a code snippet in a workshop on jclouds. Still it works
            //Code not from documentation
            Optional<? extends SecurityGroupApi> securityGroupExt = temp.getSecurityGroupExtensionForZone(region);
            System.out.println("  Security Group Support: " + securityGroupExt.isPresent());
            if (securityGroupExt.isPresent()) {
                SecurityGroupApi client = securityGroupExt.get();
                //+++++++++++++++++    
                //For each group of the security groups
                for (NodeGroup group : cluster.getNodes()) {
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
     * This method iterates over the security groups defined in the cluster file
     * It launches in parallel all the number of nodes specified in the group of the cluster file using the 
     * compute service abstraction from jclouds
     * 
     * If succesful, returns true;
     */
    public boolean launchNodesBasicSetup(Cluster cluster) {
        boolean status = false;
        try {
            TemplateBuilder kthfsTemplate = templateKTHFS(cluster, service.templateBuilder());
            //Use better Our scriptbuilder abstraction
            JHDFSScriptBuilder initScript = JHDFSScriptBuilder.builder()
                    .scriptType(JHDFSScriptBuilder.ScriptType.INIT)
                    .publicKey(publicKey)
                    .build();

            selectProviderTemplateOptions(cluster, kthfsTemplate, initScript);
            for (NodeGroup group : cluster.getNodes()) {
                messages.addMessage("Creating " + group.getNumber() + "  nodes in Security Group " + group.getSecurityGroup());
                Set<? extends NodeMetadata> ready = service.createNodesInGroup(group.getSecurityGroup(), group.getNumber(), kthfsTemplate.build());
                //For the demo, we keep track of the returned set of node Metadata launched and which group 
                //was
                messages.addMessage("Nodes created in Security Group " + group.getSecurityGroup() + " with "
                        + "basic setup");
                nodes.put(group.getSecurityGroup(), ready);

               
                //Fetch the nodes info so we can launch first mgm before the rest!
                Set<String> roles = new HashSet(group.getRoles());
                if (roles.contains("MySQLCluster*mgm")) {
                    Iterator<? extends NodeMetadata> iter = ready.iterator();
                    while (iter.hasNext()) {
                        //Add private ip to mgm
                        NodeMetadata node = iter.next();
                        mgmIP.addAll(node.getPrivateAddresses());
                        //need to also check if they have ndb
//                        if (roles.contains("MySQLCluster*ndb")) {
//                            ndbs.addAll(node.getPrivateAddresses());
//                        }
                        mgms.put(node, group.getRoles());
                    }
                    continue;
                } 
                else if(roles.contains("MySQLCluster*ndb")){
                    Iterator<? extends NodeMetadata> iter = ready.iterator();
                    while (iter.hasNext()) {
                        NodeMetadata node = iter.next();
//                        if (roles.contains("MySQLCluster*ndb")) {
                            ndbsIP.addAll(node.getPrivateAddresses());
//                        }
//                        if (roles.contains("MySQLCluster*memcached")) {
//                            mySQLClientsIP.addAll(node.getPrivateAddresses());
//                        }
                        ndbs.put(node, group.getRoles());
                    }
                }
                else if(roles.contains("MySQLCluster*mysqld")){
                    Iterator<? extends NodeMetadata> iter = ready.iterator();
                    while (iter.hasNext()) {
                        NodeMetadata node = iter.next();
//                        if (roles.contains("MySQLCluster*ndb")) {
                            mySQLClientsIP.addAll(node.getPrivateAddresses());
//                        }
//                        if (roles.contains("MySQLCluster*memcached")) {
//                            mySQLClientsIP.addAll(node.getPrivateAddresses());
//                        }
                        mysqlds.put(node, group.getRoles());
                    }
                }
                else if(roles.contains("KTHFS*namenode")){
                    Iterator<? extends NodeMetadata> iter = ready.iterator();
                    while (iter.hasNext()) {
                        NodeMetadata node = iter.next();
//                        if (roles.contains("MySQLCluster*ndb")) {
                            namenodesIP.addAll(node.getPrivateAddresses());
//                        }
//                        if (roles.contains("MySQLCluster*memcached")) {
//                            mySQLClientsIP.addAll(node.getPrivateAddresses());
//                        }
                        namenodes.put(node, group.getRoles());
                    }
                }

            }
            status = true;
        } catch (RunNodesException e) {
            System.out.println("error adding nodes to group "
                    + "ups something got wrong on the nodes");
        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
        } finally {
            return status;
        }
    }

    
    /*
     * Method to setup the nodes in the correct order for our platform in the first run
     */
    
    public void deployingConfigurations(){
        //First phase mgm configuration
        JHDFSScriptBuilder.Builder scriptBuilder = JHDFSScriptBuilder.builder()
                .mgms(mgmIP)
                .mysql(mySQLClientsIP)
                .namenodes(namenodesIP)
                .ndbs(ndbsIP)
                .privateIP(privateIP)
                .publicKey(publicKey)
                .scriptType(JHDFSScriptBuilder.ScriptType.JHDFS);
        
        Set<NodeMetadata> mgmNodes = mgms.keySet();
        //Asynchronous node launch
        Iterator<NodeMetadata> iter = mgmNodes.iterator();
        while(iter.hasNext()){
            NodeMetadata node= iter.next();
            List<String> ips = new LinkedList(node.getPrivateAddresses());
            //Listenable Future
            service.submitScriptOnNode(node.getId(), scriptBuilder.build(ips.get(0), mgms.get(node)), 
                    RunScriptOptions.NONE);
        }
        
        //Rest of the phases TODO
    }
    
    /*
     * This is the code for the demo
     * To be removed in a near future.
     */
    @Deprecated
    public void demoOnly(Cluster cluster) {
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

        JHDFSScriptBuilder.Builder jhdfsBuilder = JHDFSScriptBuilder.builder();
        //All follow the same idea, we go over the security groups and fetch
        messages.addMessage("Starting the MySQL Cluster....");
        for (NodeGroup group : cluster.getNodes()) {
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
                            .privateIP(privateIP)
                            .build());

                    //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }

        //All follow the same description
        for (NodeGroup group : cluster.getNodes()) {
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
                            .privateIP(privateIP)
                            .build());

                    //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }

        for (NodeGroup group : cluster.getNodes()) {
            if (group.getSecurityGroup().equals("mysql")) {
                messages.addMessage("Starting setup of MySQLd Node..");
                Set<? extends NodeMetadata> elements = nodes.get(group.getSecurityGroup());
                Iterator<? extends NodeMetadata> iter = elements.iterator();
                while (iter.hasNext()) {
                    NodeMetadata data = iter.next();
                    List<String> ips = new LinkedList<String>(data.getPrivateAddresses());


                    //Prepare the script
                    //We build the json chef file
                    messages.addMessage("Generating JSON chef file for node...");
                    //Append at the end of the script the need to launch the chef command and run the restart command

                    StatementList nodeConfig = new StatementList(
                            jhdfsBuilder.scriptType(JHDFSScriptBuilder.ScriptType.JHDFS)
                            .ndbs(ndbsDemo)
                            .mgms(mgmDemo)
                            .mysql(mysqlDemo)
                            .namenodes(namenodesDemo)
                            .roles(group.getRoles())
                            .nodeIP(ips.get(0))
                            .privateIP(privateIP)
                            .build());

                    //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }

        messages.addMessage("Starting Hadoop nodes...");
        for (NodeGroup group : cluster.getNodes()) {
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
                            .privateIP(privateIP)
                            .build());

                    //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }
        for (NodeGroup group : cluster.getNodes()) {
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
                            .privateIP(privateIP)
                            .build());

                    //We connect to the specific node using its metadata id and give him the json and the chef command to run
                    messages.addMessage("Connecting by SSH to node, launch chef solo and run recipes...");
                    service.runScriptOnNode(data.getId(), nodeConfig);
                }
            }
        }
    }

    private ComputeService initContext() {

        //We define the properties of our service
        Properties serviceDetails = serviceProperties();

        // example of injecting a ssh implementation
        // injecting the logging module
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule(),
                new EnterpriseConfigurationModule());

        ContextBuilder build = null;
        //We prepare the context depending of what the user selects
        switch (provider) {
            case AWS_EC2:
                build = ContextBuilder.newBuilder(provider.toString())
                        .credentials(id, key)
                        .modules(modules)
                        .overrides(serviceDetails);

                break;
            case OPENSTACK:
                build = ContextBuilder.newBuilder(provider.toString())
                        .endpoint(endpoint)
                        .credentials(id, key)
                        .modules(modules)
                        .overrides(serviceDetails);

                break;
            //Rackspace not implemented,
            case RACKSPACE:
                build = ContextBuilder.newBuilder(provider.toString())
                        .credentials(id, key)
                        .modules(modules)
                        .overrides(serviceDetails);
                break;
        }

        if (build == null) {
            throw new NullPointerException("Not selected supported provider");
        }

        ComputeServiceContext context = build.buildView(ComputeServiceContext.class);

        //From minecraft example, how to include your own event handlers
        context.utils().eventBus().register(VirtualizationController.ScriptLogger.INSTANCE);
        messages.addMessage("Virtualization context initialized, start opening security groups");
        return context.getComputeService();
    }

    /*
     * Define the service properties for the compute service context using
     * Amazon EC2 like Query parameters and regions. 
     * Does the same for Openstack and Rackspace but we dont setup anything for now
     * 
     * Includes time using the ports when launching the VM instance executing the script
     */
    private Properties serviceProperties() {
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
     * Select extra options depending of the provider we selected
     * For example we include the bootstrap script to download and do basic setup the first time
     * For openstack we override the need to generate a key pair and the user used by the image to login
     * EC2 jclouds detects the login by default
     */
    private void selectProviderTemplateOptions(Cluster cluster, TemplateBuilder kthfsTemplate, JHDFSScriptBuilder script) {

        StatementList bootstrap = new StatementList(script);
        switch (provider) {
            case AWS_EC2:
                kthfsTemplate.options(EC2TemplateOptions.Builder
                        .runScript(bootstrap));
                break;
            case OPENSTACK:
                kthfsTemplate.options(NovaTemplateOptions.Builder
                        .overrideLoginUser(cluster.getProvider().getLoginUser())
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
     * Template of the VM we want to launch using EC2, or Openstack
     */
    private TemplateBuilder templateKTHFS(Cluster cluster, TemplateBuilder template) {

        switch (provider) {
            case AWS_EC2:
                template.os64Bit(true);
                template.hardwareId(cluster.getProvider().getInstanceType());
                template.imageId(cluster.getProvider().getImage());
                template.locationId(cluster.getProvider().getRegion());
                break;
            case OPENSTACK:
                template.os64Bit(true);
                template.imageId(cluster.getProvider().getImage());
                template.hardwareId(cluster.getProvider().getRegion()
                        + "/" + cluster.getProvider().getInstanceType());
                break;
            case RACKSPACE:
                break;
            default:
                throw new AssertionError();
        }


        return template;
    }
}
