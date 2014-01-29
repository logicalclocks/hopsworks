package se.kth.kthfsdashboard.portal;

import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import static com.google.common.base.Predicates.not;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import static com.google.common.collect.Iterables.getOnlyElement;
import com.google.inject.Module;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import static org.jclouds.Constants.PROPERTY_CONNECTION_TIMEOUT;
import static org.jclouds.Constants.PROPERTY_MAX_RETRIES;
import static org.jclouds.Constants.PROPERTY_RETRY_DELAY_START;
import org.jclouds.ContextBuilder;
import org.jclouds.aws.domain.Region;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_AMI_QUERY;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_CC_AMI_QUERY;
import static org.jclouds.aws.ec2.reference.AWSEC2Constants.PROPERTY_EC2_CC_REGIONS;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_PORT_OPEN;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.withIds;
import org.jclouds.ec2.EC2AsyncClient;
import org.jclouds.ec2.EC2Client;
import org.jclouds.ec2.compute.options.EC2TemplateOptions;
import org.jclouds.ec2.domain.IpProtocol;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.domain.Ingress;
import org.jclouds.openstack.nova.v2_0.domain.SecurityGroup;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;
import org.jclouds.openstack.nova.v2_0.extensions.SecurityGroupApi;
import static org.jclouds.openstack.nova.v2_0.predicates.SecurityGroupPredicates.nameEquals;
import org.jclouds.rest.RestContext;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import static org.jclouds.scriptbuilder.domain.Statements.createOrOverwriteFile;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import org.jclouds.scriptbuilder.statements.git.InstallGit;
import org.jclouds.scriptbuilder.statements.ssh.AuthorizeRSAPublicKeys;
import org.jclouds.sshj.config.SshjSshClientModule;
import se.kth.kthfsdashboard.provision.MessageController;
import se.kth.kthfsdashboard.provision.ProviderType;

/**
 * Represents a provisioning process for a cloud machine in order to install the dashboard.
 * Contains methods that represent this process and generate the scripts to configure the dashboard.
 * 
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class CloudProvision {

    private String locationId;
    private String hardwareId;
    private String imageId;
    private String loginUser;
    private String keystoneEndpoint;
    private String provider;
    private String id;
    private String key;
    private String publicKey;
    private String group;
    private boolean loginUserEnabled;
    private boolean publicKeyEnabled;
    private MessageController messages;
    private ComputeService service;

    public CloudProvision(PortalMB providerMB, MessageController messages) {
        if (providerMB.getProviderName().equals("Amazon-EC2")) {
            provider = ProviderType.AWS_EC2.toString();
        }
        if (providerMB.getProviderName().equals("OpenStack")) {
            provider = ProviderType.OPENSTACK.toString();
            keystoneEndpoint = providerMB.getKeystone();
        }
        if (providerMB.getProviderName().equals("Rackspace")) {
            provider = ProviderType.RACKSPACE.toString();

        }
        if (providerMB.isEnableLoginUser()) {
            loginUser = providerMB.getLoginUser();
        }
        this.id = providerMB.getId();
        this.key = providerMB.getKey();
        this.locationId = providerMB.getLocationId();
        this.imageId = providerMB.getImageId();
        this.hardwareId = providerMB.getHardwareId();
        this.publicKey = providerMB.getPublicKey();
        this.publicKeyEnabled = providerMB.isPublicKeyEnabled();
        this.loginUserEnabled = providerMB.isEnableLoginUser();
        this.group = providerMB.getGroupName();
        this.messages = messages;
    }

    public void initComputeService() {
        ProviderType check = ProviderType.fromString(provider);
        //We define the properties of our service
        Properties serviceDetails = serviceProperties(check);

        // example of injecting a ssh implementation
        // injecting the logging module
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule(),
                new EnterpriseConfigurationModule());

        ContextBuilder build = null;
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

        ComputeServiceContext context = build.buildView(ComputeServiceContext.class);

        //From minecraft example, how to include your own event handlers!
        context.utils().eventBus().register(CloudProviderController.ScriptLogger.INSTANCE);

        service = context.getComputeService();
    }

    public void launchInstance() {

        messages.addMessage("Configuring service context for " + provider);

        try {

            TemplateBuilder kthfsTemplate = templateKTHFS(service.templateBuilder());

            messages.addMessage("Configuring bootstrap script and specifying ports for security group");

            selectProviderTemplateOptions(kthfsTemplate);

            messages.addMessage("Security Group Ports [22, 80, 8080, 8181, 8686, 8983, 4848, 4040, 4000, 443]");
            createSecurityGroups(service);
            NodeMetadata node = getOnlyElement(service.createNodesInGroup(group, 1, kthfsTemplate.build()));
            String address = "";
            String privateIP = "";
            if (provider.equals("openstack-nova")) {
                address = allocatePublicIP(service, node);
            } else {
                //EC2
                System.out.println(node.getPublicAddresses());
                for (String ip : node.getPublicAddresses()) {
                    address = ip;
                    break;
                }
            }
            for (String ip : node.getPrivateAddresses()) {
                privateIP = ip;
                break;
            }


            service.runScriptOnNodesMatching(
                    Predicates.<NodeMetadata>and(not(TERMINATED), withIds(node.getId())),
                    initBootstrapScript(), RunScriptOptions.Builder.nameTask("initbootstrap")
                    .overrideLoginCredentials(node.getCredentials()));
            messages.addSuccessMessage("http://" + address + ":8080/hop-dashboard, Private IP: " + privateIP);
            messages.setDashboardURL("http://" + address + ":8080/hop-dashboard");
            messages.setDashboardPrivateIP(privateIP);
            messages.addMessage("VM launched with private IP: " + privateIP + ", public IP: " + address);
            messages.addMessage("Running chef-solo, deploying Architecture");
            service.runScriptOnNodesMatching(
                    Predicates.<NodeMetadata>and(not(TERMINATED), withIds(node.getId())), runChefSolo(privateIP),
                    RunScriptOptions.Builder.nameTask("runchef-solo")
                    .overrideLoginCredentials(node.getCredentials()));
            messages.clearMessages();

        } catch (RunNodesException e) {
            messages.addErrorMessage("error adding node to group " + group
                    + "ups something got wrong on the node");
            messages.clearMessages();
        } catch (Exception e) {
            messages.addErrorMessage("Error: " + e.getMessage());
            messages.clearMessages();
        } finally {
            service.getContext().close();
        }

    }

    public void destroyInstance(String nodeId) {
        messages.addMessage("Preparing to destroy instance " + nodeId);
        messages.addMessage("Configuring service context...");

        try {
            messages.addMessage("Sending Destroy Request");
            service.destroyNode(nodeId);
            messages.addSuccessMessage("Node destroyed succesfully");
        } catch (Exception e) {
            //System.err.println("error: " + e.getMessage());
            messages.addErrorMessage("error: " + e.getMessage());
        } finally {
            service.getContext().close();
        }
    }
    /*
     * Template of the VM we want to launch using EC2, or Openstack
     */

    private TemplateBuilder templateKTHFS(TemplateBuilder template) {
        ProviderType check = ProviderType.fromString(provider);
        switch (check) {
            case AWS_EC2:
                template.os64Bit(true);
                template.hardwareId(hardwareId);
                template.imageId(imageId);
                template.locationId(locationId);
                break;
            case OPENSTACK:
                template.os64Bit(true);
                template.imageId(locationId + "/" + imageId);
                template.hardwareId(locationId + "/" + hardwareId);
                break;
            case RACKSPACE:
                break;
            default:
                throw new AssertionError();

        }


        return template;
    }

    /*
     * Select extra options depending of the provider we selected
     */
    private void selectProviderTemplateOptions(TemplateBuilder kthfsTemplate) {
        ProviderType check = ProviderType.fromString(provider);

        switch (check) {
            case AWS_EC2:
                if (loginUserEnabled) {
                    kthfsTemplate.options(EC2TemplateOptions.Builder
                            .overrideLoginUser(loginUser));
                }

                break;
            case OPENSTACK:
                kthfsTemplate.options(NovaTemplateOptions.Builder
                        //.inboundPorts(22, 80, 8080, 8181, 8686, 8983, 4848, 4040, 4000, 443)
                        .overrideLoginUser(loginUser)
                        .generateKeyPair(true));
                break;
            case RACKSPACE:

                break;
            default:
                throw new AssertionError();
        }

    }

    private void createSecurityGroups(ComputeService service) {
        int[] portsTCP = {443, 22, 80, 3306, 8080, 8181, 8686, 8090, 8983, 4848, 4040, 4000, 40102};

        if (provider.equals(ProviderType.AWS_EC2.toString())) {
            RestContext<EC2Client, EC2AsyncClient> temp = service.getContext().unwrap();
            EC2Client client = temp.getApi();
            String groupName = "jclouds#" + group;
            System.out.printf("%d: creating security group: %s%n", System.currentTimeMillis(), groupName);

            //create the group
            try {
                client.getSecurityGroupServices().createSecurityGroupInRegion(locationId, groupName, groupName);
            } catch (Exception e) {
                //Group already exists
                System.out.println("Group already exists, continue");
            }
            //open the ports
            for (int i : portsTCP) {
                try {
                    client.getSecurityGroupServices().authorizeSecurityGroupIngressInRegion(locationId,
                            groupName, IpProtocol.TCP, i, i, "0.0.0.0/0");
                } catch (Exception e) {
                    System.out.println("Rule already open");
                    continue;
                }
            }

            try {
                client.getSecurityGroupServices().authorizeSecurityGroupIngressInRegion(locationId,
                        groupName, IpProtocol.UDP, 25826, 25826, "0.0.0.0/0");
            } catch (Exception e) {
                System.out.println("Rule already open");
            }
        }


        if (provider.equals(ProviderType.OPENSTACK.toString())) {
            RestContext<NovaApi, NovaAsyncApi> temp = service.getContext().unwrap();
            //+++++++++++++++++
            //This stuff below is weird, founded in a code snippet in a workshop on jclouds. Still it works
            //Code not from documentation
            Optional<? extends SecurityGroupApi> securityGroupExt = temp.getApi().getSecurityGroupExtensionForZone(locationId);
            System.out.println("  Security Group Support: " + securityGroupExt.isPresent());
            String groupName = "jclouds-" + group; //jclouds way of defining groups
            if (securityGroupExt.isPresent()) {
                SecurityGroupApi client = securityGroupExt.get();
                System.out.printf("%d: creating security group: %s%n", System.currentTimeMillis(), groupName);
                if (!client.list().anyMatch(nameEquals(groupName))) {
                    SecurityGroup created = client.createWithDescription(groupName, groupName);
                    for (int i : portsTCP) {
                        Ingress ingress = Ingress.builder()
                                .fromPort(i)
                                .toPort(i)
                                .ipProtocol(org.jclouds.openstack.nova.v2_0.domain.IpProtocol.TCP)
                                .build();
                        client.createRuleAllowingCidrBlock(created.getId(), ingress, "0.0.0.0/0");
                    }
                    Ingress ingress = Ingress.builder()
                            .fromPort(25826)
                            .toPort(25826)
                            .ipProtocol(org.jclouds.openstack.nova.v2_0.domain.IpProtocol.UDP)
                            .build();
                    client.createRuleAllowingCidrBlock(created.getId(), ingress, "0.0.0.0/0");
                }
            }
        }
    }

    private String allocatePublicIP(ComputeService service, NodeMetadata node) {
        RestContext<NovaApi, NovaAsyncApi> temp = service.getContext().unwrap();
        Optional<? extends FloatingIPApi> floatingExt = temp.getApi().getFloatingIPExtensionForZone(locationId);
        System.out.println(" Floating IP Support: " + floatingExt.isPresent());
        String ip = "";
        if (floatingExt.isPresent()) {
            FloatingIPApi client = floatingExt.get();
            System.out.printf("%d: allocating public ip: %s%n", System.currentTimeMillis(), node.getId());

            FluentIterable<? extends FloatingIP> freeIPS = client.list();
            if (!freeIPS.isEmpty()) {
                ip = freeIPS.first().get().getIp();
                System.out.println(ip);
                client.addToServer(ip, node.getProviderId());
                node = NodeMetadataBuilder.fromNodeMetadata(node).publicAddresses(ImmutableSet.of(ip)).build();
                System.out.println("Successfully associated an IP address " + ip + " for node with id: " + node.getId());
            }

        }
        return ip;
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

    /*
     * Bootscrap Script for the nodes to launch the KTHFS Dashboard and install Chef Solo
     */
    private StatementList initBootstrapScript() {

        ImmutableList.Builder<Statement> bootstrapBuilder = ImmutableList.builder();
        //bootstrapBuilder.add(exec("sudo dpkg --configure -a"));
        bootstrapBuilder.add(exec("sudo apt-key update -qq"));
        bootstrapBuilder.add(exec("sudo apt-get update -qq;"));
        bootstrapBuilder.add(exec("sudo apt-get install make;"));
        bootstrapBuilder.add(exec("sudo apt-get update -qq;"));
        bootstrapBuilder.add(exec("sudo apt-get install -f -y -qq --force-yes ruby1.9.1-full;"));
        if (publicKeyEnabled) {
            List<String> keys = new ArrayList();
            keys.add(publicKey);
            bootstrapBuilder.add(new AuthorizeRSAPublicKeys(keys));
        }

        bootstrapBuilder.add(exec("curl -L https://www.opscode.com/chef/install.sh | sudo bash"));
        InstallGit git = new InstallGit();
        bootstrapBuilder.add(git);
        bootstrapBuilder.add(exec("sudo mkdir /etc/chef;"));
        bootstrapBuilder.add(exec("cd /etc/chef;"));

        List<String> soloLines = new ArrayList<String>();
        soloLines.add("file_cache_path \"/tmp/chef-solo\"");
        soloLines.add("cookbook_path \"/tmp/chef-solo/cookbooks\"");
        bootstrapBuilder.add(createOrOverwriteFile("/etc/chef/solo.rb", soloLines));
        bootstrapBuilder.add(exec("sudo mkdir -p /tmp/chef-solo;"));

        bootstrapBuilder.add(exec("git config --global user.name \"Jim Dowling\";"));
        bootstrapBuilder.add(exec("git config --global user.email \"jdowling@sics.se\";"));
        bootstrapBuilder.add(exec("git config --global http.sslVerify false;"));
        bootstrapBuilder.add(exec("git config --global http.postBuffer 524288000;"));
        bootstrapBuilder.add(exec("sudo git clone https://github.com/hopstart/hop-chef.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://github.com/hopstart/hop-chef.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://github.com/hopstart/hop-chef.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://github.com/hopstart/hop-chef.git /tmp/chef-solo/;"));
        bootstrapBuilder.add(exec("sudo git clone https://github.com/hopstart/hop-chef.git /tmp/chef-solo/;"));

        return new StatementList(bootstrapBuilder.build());
    }

    /*
     * Script to run Chef Solo
     */
    private StatementList runChefSolo(String ip) {
        ImmutableList.Builder<Statement> bootstrapBuilder = ImmutableList.builder();
        createNodeConfiguration(bootstrapBuilder, ip);
        bootstrapBuilder.add(exec("sudo chef-solo -c /etc/chef/solo.rb -j /etc/chef/chef.json"));
        return new StatementList(bootstrapBuilder.build());
    }

    private StringBuilder generateConfigJSON(List<String> runList, String ip) {
        //Start json
        StringBuilder json = new StringBuilder();
        //Open json bracket
        json.append("{");
        //mysql
        json.append("\"mysql\":{\"user\":\"root\",\"user_password\":\"kthfs\","
                + "\"server_debian_password\":\"kthfs\",\"server_root_password\":\"kthfs\","
                + "\"server_repl_password\":\"kthfs\",\"bind_address\":\"")
                //.append(ip)
                .append("localhost")
                .append("\"},");
        //collectd server
        json.append("\"collectd\":{\"server\":\"").append(ip).append("\"},");
        //Paascredentials
        json.append("\"dashboard\":{\"private_ip\":\"")
                .append(ip)
                .append("\",\"public_key\":\"")
                //.append(publicKey)
                .append("\"},");
        json.append("\"provider\":{\"name\":\"")
                .append(provider)
                .append("\",\"access_key\":\"")
                .append(key)
                .append("\",\"account_id\":\"")
                .append(id)
                .append("\",\"keystone_url\":\"")
                .append(keystoneEndpoint)
                .append("\"},");
        //ndb enabled false
        json.append("\"ndb\":{\"enabled\":\"false\"},");
        //Recipe runlist append in the json
        json.append("\"run_list\":[");
        for (int i = 0; i < runList.size(); i++) {
            if (i == runList.size() - 1) {
                json.append("\"").append(runList.get(i)).append("\"");
            } else {
                json.append("\"").append(runList.get(i)).append("\",");
            }
        }
        //close the json
        json.append("]}");
        return json;
    }

    private void createNodeConfiguration(ImmutableList.Builder<Statement> statements, String ip) {
        //First we generate the recipe runlist based on the roles defined in the security group of the cluster
        List<String> runlist = createRunList();
        //Start json
        StringBuilder json = generateConfigJSON(runlist, ip);
        //Create the file in this directory in the node
        statements.add(exec("sudo apt-get clean;"));
        statements.add(exec("sudo apt-get update;"));
        statements.add(createOrOverwriteFile("/etc/chef/chef.json", ImmutableSet.of(json.toString())));
    }

    private List<String> createRunList() {
        List<String> list = new ArrayList<String>();
        list.add("authbind");
        list.add("java");
        list.add("collectd::server");
//        list.add("mysql::server");
//        list.add("mysql::client");
        list.add("ndb::install");
        list.add("ndb::mysqld");
        list.add("openssh");
        list.add("openssl");
        list.add("glassfish::populate-dbs");
        list.add("glassfish::kthfs");
        return list;
    }
}
