package se.kth.hop.deploy.baremetal;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.ejb.EJB;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import se.kth.kthfsdashboard.host.Host;
import se.kth.kthfsdashboard.host.HostEJB;
import se.kth.hop.deploy.provision.DeploymentPhase;
import se.kth.hop.deploy.provision.DeploymentProgressFacade;
import se.kth.hop.deploy.provision.MessageController;
import se.kth.hop.deploy.provision.NodeStatusTracker;
import se.kth.hop.deploy.provision.Provision;
import se.kth.hop.deploy.provision.ProvisionController;
import se.kth.hop.deploy.provision.ScriptBuilder;
import se.kth.hop.deploy.provision.StoreResults;
import se.kth.hop.deploy.virtualization.parser.Baremetal;
import se.kth.hop.deploy.virtualization.parser.BaremetalGroup;

/**
 *Representation of a Baremetal virtualization process
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class BaremetalClusterProvision implements Provision {

    @EJB
    private HostEJB hostEJB;
    @EJB
    private DeploymentProgressFacade progressEJB;
    private static final int RETRIES = 5;
    private String privateKey;
    private String publicKey;
    private String privateIP;
    private Baremetal cluster;
    private MessageController messages;
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

    public BaremetalClusterProvision(ProvisionController controller) {
        this.privateIP = controller.getPrivateIP();
        this.publicKey = controller.getPublicKey();
        this.messages = controller.getMessages();
        this.privateKey = controller.getPrivateKey();
        this.progressEJB = controller.getDeploymentProgressFacade();
        this.hostEJB = controller.getHostEJB();
        this.cluster = controller.getBaremetalCluster();
    }

    @Override
    public void initializeCluster() {
        progressEJB.createProgress(cluster,privateKey);
    }

    @Override
    public boolean launchNodesBasicSetup() {
        boolean status = false;
        System.out.println("Starting the creation phase");
        latch = new CountDownLatch(cluster.getTotalHosts());
        pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(cluster.getTotalHosts()));
        final ScriptBuilder initScript = ScriptBuilder.builder()
                .scriptType(ScriptBuilder.ScriptType.INIT)
                .publicKey(publicKey)
                .gitRepo(cluster.getGlobal().getGit().getRepository())
                .gitName(cluster.getGlobal().getGit().getUser())
                .build();

        for (final BaremetalGroup group : cluster.getNodes()) {

            messages.addMessage("Creating " + group.getNumber()
                    + "  nodes in Security Group " + group.getServices().get(0));
            //Identify the biggest group
            max = max < group.getNumber() ? group.getNumber() : max;
            
            for (final String host : group.getHosts()) {
                try {
                    progressEJB.initializeHostProgress(host);
                    //baremetal we set all the values to the ip of the node
                    Host newHost = new Host();
                    newHost.setHostname(host);
                    newHost.setHostId(host);
                    newHost.setPublicIp(host);
                    newHost.setPrivateIp(host);
                    hostEJB.storeHost(newHost, true);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error updating the DataBase");
                }
                //Generate function to store results when done
                List<String> services = new LinkedList<String>();
                services.addAll(group.getServices());

                final StoreResults results = new StoreResults(services, latch, this);
                ListenableFuture<Set<NodeMetadata>> groupCreation =
                        pool.submit(new InitializeBaremetalCallable(privateKey, host, cluster.getLoginUser(),
                        initScript, nodes, group, messages));

                Futures.transform(groupCreation, results);
            }

        }
        try {
            latch.await(cluster.getTotalHosts() * 30, TimeUnit.MINUTES);
            progressEJB.updateCreationCluster(cluster.getName());
            status = true;
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
        ScriptBuilder.Builder scriptBuilder =
                ScriptBuilder.builder().scriptType(ScriptBuilder.ScriptType.INSTALLBAREMETAL);
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
        
        System.out.println("Starting Install phase on the machines");
        nodeInstall(groupLaunch, scriptBuilder, RETRIES);
    }

    @Override
    public void deployingConfigurations() {
        //create pool of threads taking the biggest cluster
        System.out.println("Starting Configuration script on the machines.");
        pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(max * 2));
        //latch = new CountDownLatch(mgmNodes.size());
        //First phase mgm configuration
        ScriptBuilder.Builder scriptBuilder = ScriptBuilder.builder()
                .mgms(mgmIP)
                .mysql(mySQLClientsIP)
                .namenodes(namenodesIP)
                .ndbs(ndbsIP)
                .privateIP(privateIP)
                .publicKey(publicKey)
                .clusterName(cluster.getName())
                .scriptType(ScriptBuilder.ScriptType.CONFIGBAREMETAL);

        //Asynchronous node launch
        //launch mgms
        Set<NodeMetadata> groupLaunch = mgms.keySet();
        messages.addMessage("Configuring mgm nodes");

        //Update view state to configure
        persistState(groupLaunch, DeploymentPhase.CONFIGURE);
        nodePhase(groupLaunch, mgms, scriptBuilder, RETRIES);
        

        //launch ndbs
        groupLaunch = ndbs.keySet();
        messages.addMessage("Configuring ndb nodes");

        persistState(groupLaunch, DeploymentPhase.CONFIGURE);
        nodePhase(groupLaunch, ndbs, scriptBuilder, RETRIES);
        

        //launch mysqlds
        groupLaunch = mysqlds.keySet();
        messages.addMessage("Configuring mysqld nodes");

        persistState(groupLaunch, DeploymentPhase.CONFIGURE);
        nodePhase(groupLaunch, mysqlds, scriptBuilder, RETRIES);
        

        //launch namenodes
        groupLaunch = namenodes.keySet();
        messages.addMessage("Configuring namenodes");

        persistState(groupLaunch, DeploymentPhase.CONFIGURE);
        nodePhase(groupLaunch, namenodes, scriptBuilder, RETRIES);
        

        //launch datanodes
        groupLaunch = datanodes.keySet();
        messages.addMessage("Configuring datanodes");

        persistState(groupLaunch, DeploymentPhase.CONFIGURE);
        nodePhase(groupLaunch, datanodes, scriptBuilder, RETRIES);
        
    }

    @Override
    public List<String> getNdbsIP() {
        return ndbsIP;
    }

    @Override
    public List<String> getMgmIP() {
        return mgmIP;
    }

    @Override
    public List<String> getMySQLClientIP() {
        return mySQLClientsIP;
    }

    @Override
    public List<String> getNamenodesIP() {
        return namenodesIP;
    }

    @Override
    public Map<NodeMetadata, List<String>> getMgms() {
        return mgms;
    }

    @Override
    public Map<NodeMetadata, List<String>> getNdbs() {
        return ndbs;
    }

    @Override
    public Map<NodeMetadata, List<String>> getMysqlds() {
        return mysqlds;
    }

    @Override
    public Map<NodeMetadata, List<String>> getNamenodes() {
        return namenodes;
    }

    @Override
    public Map<NodeMetadata, List<String>> getDatanodes() {
        return datanodes;
    }
       

    private void nodePhase(Set<NodeMetadata> nodes, Map<NodeMetadata, List<String>> map,
            ScriptBuilder.Builder scriptBuilder, int retries) {

        pendingNodes = new CopyOnWriteArraySet<NodeMetadata>(nodes);
        
        while (!pendingNodes.isEmpty() && retries != 0) {
            latch = new CountDownLatch(pendingNodes.size());
            Iterator<NodeMetadata> iter = pendingNodes.iterator();
            while (iter.hasNext()) {
                final NodeMetadata node = iter.next();
                System.out.println(node.toString());
                List<String> ips = new LinkedList(node.getPrivateAddresses());

                //launch the nodephase of that node and generate the script to be run. If future succeeds
                //We will update the Database.
                String nodeId = node.getId();
                ScriptBuilder script = scriptBuilder.build(ips.get(0), map.get(node),
                        nodeId);
                ListenableFuture<ExecResponse> future = pool.submit(
                        new SubmitScriptBaremetalCallable(node, script));

                future.addListener(new NodeStatusTracker(node, latch, pendingNodes,
                        future), pool);
            }
            try {
                //wait for all the works to finish, 25 min estimated for each node +30 min extra margin to give
                //some extra time.
                latch.await(25 * nodes.size() + 60, TimeUnit.MINUTES);
                messages.addMessage("Launch phase complete...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                //Update the nodes that have finished the install phase
                Set<NodeMetadata> complete = new HashSet<NodeMetadata>(nodes);
                if (!pendingNodes.isEmpty()) {
                    
                    Set<NodeMetadata> remain =pendingNodes;
                    //Mark the nodes that are been reinstalled and completed
                    complete.removeAll(remain);
                    persistState(complete, DeploymentPhase.COMPLETE);
                    persistState(remain, DeploymentPhase.RETRYING);
                    System.out.println("Retrying Nodes in configuration phase");
                    --retries;
                }
                else{
                    persistState(complete, DeploymentPhase.COMPLETE);
                }

            }

        }
        if(retries==0&&!pendingNodes.isEmpty()){
            persistState(pendingNodes, DeploymentPhase.ERROR);
        }
    }

    private void nodeInstall(Set<NodeMetadata> nodes, ScriptBuilder.Builder scriptBuilder, int retries) {
        pendingNodes = new CopyOnWriteArraySet<NodeMetadata>(nodes);
        while (retries != 0 && !pendingNodes.isEmpty()) {
            latch = new CountDownLatch(pendingNodes.size());
            Iterator<NodeMetadata> iter = pendingNodes.iterator();
            while (iter.hasNext()) {
                final NodeMetadata node = iter.next();

                final ScriptBuilder script = scriptBuilder.build();
                //Generate install scripts and lauun all the nodes in parallel. If future succeeds we 
                //update database 
                ListenableFuture<ExecResponse> future = pool.submit(
                        new SubmitScriptBaremetalCallable(node, script));

                future.addListener(new NodeStatusTracker(node, latch, pendingNodes,
                        future), pool);
            }
            try {
                //wait for all the works to finish, 25 min estimated for each node +30 min extra margin to give
                //some extra time.
                latch.await(25 * nodes.size() + 60, TimeUnit.MINUTES);
                messages.addMessage("Install phase complete...");
                
            } catch (InterruptedException e) {

                e.printStackTrace();
            } finally {
                //Update the nodes that have finished the install phase
                Set<NodeMetadata> complete = new HashSet<NodeMetadata>(nodes);
                
                if (!pendingNodes.isEmpty()) {
                    Set<NodeMetadata> remain = pendingNodes;
                    //Mark the nodes that are been reinstalled and completed
                    complete.removeAll(remain);
                    persistState(complete, DeploymentPhase.WAITING);
                    persistState(remain, DeploymentPhase.RETRYING);
                    --retries;
                    System.out.println("Retrying Nodes in Install phase");
                }
                else{
                    persistState(complete, DeploymentPhase.WAITING);
                }
            }

        }
        if(retries==0&&!pendingNodes.isEmpty()){
            persistState(pendingNodes, DeploymentPhase.ERROR);
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

}