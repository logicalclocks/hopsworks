/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.ejb.EJB;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.scriptbuilder.domain.StatementList;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.kthfsdashboard.virtualization.clusterparser.Baremetal;
import se.kth.kthfsdashboard.virtualization.clusterparser.BaremetalGroup;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class BaremetalClusterProvision implements Provision {

    @EJB
    private HostEJB hostEJB;
    @EJB
    private DeploymentProgressFacade progressEJB;
    private static final int RETRIES = 2;
    private String privateKey;
    private String publicKey;
    private String privateIP;
    private Baremetal cluster;
    private MessageController messages;
    //private Map<String, Set<? extends NodeMetadata>> nodes = new HashMap();
    private Map<String, Set<? extends NodeMetadata>> nodes =
            new ConcurrentHashMap<String, Set<? extends NodeMetadata>>();
    private Map<NodeMetadata, List<String>> mgms = new HashMap();
    private Map<NodeMetadata, List<String>> ndbs = new HashMap();
    private Map<NodeMetadata, List<String>> mysqlds = new HashMap();
    private Map<NodeMetadata, List<String>> namenodes = new HashMap();
    private Map<NodeMetadata, List<String>> datanodes = new HashMap();
    private List<String> ndbsIP = new LinkedList();
    private List<String> mgmIP = new LinkedList();
    private List<String> mySQLClientsIP = new LinkedList();
    private List<String> namenodesIP = new LinkedList();
    private ListeningExecutorService pool;
    private CountDownLatch latch;
    private CopyOnWriteArraySet<NodeMetadata> pendingNodes;
    private int max = 0;
    private SSHClient client;
    private KeyProvider keys;

    public BaremetalClusterProvision(VirtualizationController controller) {
        this.privateIP = controller.getPrivateIP();
        this.publicKey = controller.getPublicKey();
        this.messages = controller.getMessages();

        this.progressEJB = controller.getDeploymentProgressFacade();
        this.hostEJB = controller.getHostEJB();
        this.cluster = controller.getBaremetalCluster();
    }

    @Override
    public void initializeCluster() {
        progressEJB.createProgress(cluster);
        try {
            keys = client.loadKeys(privateKey, null, null);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error loading the private Key");
        }
    }

    @Override
    public boolean launchNodesBasicSetup() {
        boolean status = false;
        latch = new CountDownLatch(cluster.getTotalHosts());
        pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(cluster.getTotalHosts()));
        final JHDFSScriptBuilder initScript = JHDFSScriptBuilder.builder()
                .scriptType(JHDFSScriptBuilder.ScriptType.INIT)
                .publicKey(publicKey)
                .build();

        for (final BaremetalGroup group : cluster.getNodes()) {
            messages.addMessage("Creating " + group.getNumber()
                    + "  nodes in Security Group " + group.getSecurityGroup());
            //Identify the biggest group
            max = max < group.getNumber() ? group.getNumber() : max;
            //iterate over the hosts
            for (final String host : group.getHosts()) {
                //Generate function to store results when done
                final StoreResults results = new StoreResults(group.getRoles(), latch);
                ListenableFuture<Set<NodeMetadata>> groupCreation =
                        pool.submit(new InitializeBaremetalCallable(privateKey, host, cluster.getLoginUser(), 
                        initScript, nodes, group, messages));
                        
//                        new Callable<Set<NodeMetadata>>() {
//                    @Override
//                    public Set<NodeMetadata> call() {
//                        Set<NodeMetadata> ready = new HashSet<NodeMetadata>();
//                        client = new SSHClient();
//                        client.addHostKeyVerifier(new PromiscuousVerifier());
//                        try {
//                            keys = client.loadKeys(privateKey, null, null);
//
//                            client.connect(host);
//                            client.authPublickey(cluster.getLoginUser(), keys);
//                            final Session session = client.startSession();
//                            final Session.Command cmd = session.exec(initScript.render(OsFamily.UNIX));
//                            System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
//                            System.out.println("\n** exit status: " + cmd.getExitStatus());
//                            NodeMetadataBuilder builder = new NodeMetadataBuilder();
//                            Set<String> publicIP = new HashSet<String>();
//                            publicIP.add(host);
//                            LoginCredentials credentials = LoginCredentials.builder().user(cluster.getLoginUser())
//                                    .privateKey(privateKey).noPassword().build();
//                            Location location = new LocationBuilder().id("KTHFS")
//                                    .description("RandomRegion").scope(LocationScope.HOST).build();
//                            NodeMetadata node = builder.hostname("193.10.64.127").id("KTHFS").location(location)
//                                    .loginPort(22)
//                                    .name("BareMetalNode").credentials(credentials).ids("KTHFS").providerId("Physical")
//                                    .status(NodeMetadata.Status.RUNNING)
//                                    .publicAddresses(publicIP).privateAddresses(publicIP).build();
//                            ready.add(node);
//
//
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            System.out.println("Error loading the private Key");
//                        } finally {
//                            nodes.put(group.getSecurityGroup(), ready);
//                            messages.addMessage("Nodes created in Security Group " + group.getSecurityGroup() + " with "
//                                    + "basic setup");
//                            return ready;
//                        }
//                    }
//                });
                Futures.transform(groupCreation, results);
            }

        }
        try {
            latch.await(cluster.getTotalHosts() * 30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            System.out.println("Failed to launch init script in some machines");
            status = false;
        } finally {
            return status;
        }
    }

    @Override
    public void installPhase() {
        //We specify a thread pool with the same number of nodes in the system and resources are
        //Total Nodes*2
        pool = MoreExecutors.listeningDecorator(
                Executors.newFixedThreadPool((int) (cluster.getTotalHosts() * 2)));
        JHDFSScriptBuilder.Builder scriptBuilder =
                JHDFSScriptBuilder.builder().scriptType(JHDFSScriptBuilder.ScriptType.INSTALL);
        Set<NodeMetadata> groupLaunch = new HashSet<NodeMetadata>(mgms.keySet());
        groupLaunch.addAll(ndbs.keySet());
        groupLaunch.addAll(mysqlds.keySet());
        groupLaunch.addAll(namenodes.keySet());
        groupLaunch.addAll(datanodes.keySet());
        messages.addMessage("Configuring installation phase in all nodes");
        messages.addMessage("Running install process of software");
        try {
            progressEJB.updatePhaseProgress(groupLaunch, DeploymentPhase.INSTALL);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error updating in the DataBase");
        }
        nodeInstall(groupLaunch, scriptBuilder, RETRIES);
    }

    @Override
    public void deployingConfigurations() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void nodeInstall(Set<NodeMetadata> nodes, JHDFSScriptBuilder.Builder scriptBuilder, int retries) {
        //Iterative Approach
        pendingNodes = new CopyOnWriteArraySet<NodeMetadata>(nodes);
        while (retries != 0 && !pendingNodes.isEmpty()) {
            //Initialize countdown latch
            latch = new CountDownLatch(pendingNodes.size());
            Iterator<NodeMetadata> iter = pendingNodes.iterator();
            while (iter.hasNext()) {
                final NodeMetadata node = iter.next();
                //Listenable Future our own for baremetal
                final JHDFSScriptBuilder script = scriptBuilder.build();
                ListenableFuture<ExecResponse> future = pool.submit(new InstallBaremetalCallable(node, script));

//                service.submitScriptOnNode(node.getId(), new StatementList(script),
//                        RunScriptOptions.Builder.overrideAuthenticateSudo(true).overrideLoginCredentials(node.getCredentials()));
//              
                future.addListener(new NodeStatusTracker(node, latch, pendingNodes,
                        future), pool);
            }
            try {
                //wait for all the works to finish, 25 min estimated for each node +30 min extra margin to give
                //some extra time.
                latch.await(25 * nodes.size() + 60, TimeUnit.MINUTES);
                messages.addMessage("Install phase complete...");
                //error 129 openstack
//                    if (!pendingNodes.isEmpty()) {
//                        Set<NodeMetadata> remain = new HashSet<NodeMetadata>(pendingNodes);
//                        //Mark the nodes that are been reinstalled
//                        persistState(remain, DeploymentPhase.RETRYING);
//                        --retries;
//                    }
            } catch (InterruptedException e) {

//                    if (!pendingNodes.isEmpty()) {
//                        Set<NodeMetadata> remain = new HashSet<NodeMetadata>(pendingNodes);
//                        //Mark the nodes that are been reinstalled
//                        persistState(remain, DeploymentPhase.RETRYING);
//                        --retries;
//                    }
                e.printStackTrace();
            } finally {
                //Update the nodes that have finished the install phase
                if (!pendingNodes.isEmpty()) {
                    Set<NodeMetadata> remain = new HashSet<NodeMetadata>(pendingNodes);
                    //Mark the nodes that are been reinstalled
                    persistState(remain, DeploymentPhase.RETRYING);
                    --retries;
                }
                try {
                    nodes.removeAll(pendingNodes);
                    progressEJB.updatePhaseProgress(nodes, DeploymentPhase.WAITING);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error updating Database");
                }
            }

        }
    }

    /*
     * Persist state
     */
    private void persistState(Set<NodeMetadata> groupLaunch, DeploymentPhase phase) {
        try {
            progressEJB.updatePhaseProgress(groupLaunch, phase);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error Updating Database");
        }
    }

    private class StoreResults implements Function<Set<? extends NodeMetadata>, Void> {

        CountDownLatch latch;
        List<String> rolesList;

        public StoreResults(List<String> roles, CountDownLatch latch) {
            this.rolesList = roles;
            this.latch = latch;
        }

        @Override
        public Void apply(Set<? extends NodeMetadata> input) {

            Set<String> roles = new HashSet(rolesList);
            if (roles.contains("MySQLCluster-mgm")) {
                Iterator<? extends NodeMetadata> iter = input.iterator();
                while (iter.hasNext()) {
                    //Add private ip to mgm
                    NodeMetadata node = iter.next();
                    mgmIP.addAll(node.getPrivateAddresses());
                    mgms.put(node, rolesList);
                }
            } else if (roles.contains("MySQLCluster-ndb")) {
                Iterator<? extends NodeMetadata> iter = input.iterator();
                while (iter.hasNext()) {
                    NodeMetadata node = iter.next();
                    ndbsIP.addAll(node.getPrivateAddresses());
                    ndbs.put(node, rolesList);
                }
            } else if (roles.contains("MySQLCluster-mysqld")) {
                Iterator<? extends NodeMetadata> iter = input.iterator();
                while (iter.hasNext()) {
                    NodeMetadata node = iter.next();
                    mySQLClientsIP.addAll(node.getPrivateAddresses());
                    mysqlds.put(node, rolesList);
                }
            } else if (roles.contains("KTHFS-namenode")) {
                Iterator<? extends NodeMetadata> iter = input.iterator();
                while (iter.hasNext()) {
                    NodeMetadata node = iter.next();
                    namenodesIP.addAll(node.getPrivateAddresses());
                    namenodes.put(node, rolesList);
                }
            } else if (roles.contains("KTHFS-datanode")) {
                Iterator<? extends NodeMetadata> iter = input.iterator();
                while (iter.hasNext()) {
                    NodeMetadata node = iter.next();
                    datanodes.put(node, rolesList);
                }
            }
            latch.countDown();
            return null;
        }
    }
}