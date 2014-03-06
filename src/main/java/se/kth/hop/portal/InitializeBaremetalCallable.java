/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.portal;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.scriptbuilder.domain.StatementList;
import se.kth.hop.deploy.provision.MessageController;

/**
 * Asynchronous Thread that handles SSH session to the node and executes the 
 * given initialization script.
 * 
 *  It returns back the ExecResponse which wraps the result of executing the SSH session.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */

public class InitializeBaremetalCallable implements Callable<Set<NodeMetadata>>{
    private SSHClient client;
    private KeyProvider keys;
    private String privateKey;
    private String host;
    private String loginUser;
    private StatementList initScript;
    private Map<String, Set<? extends NodeMetadata>> nodes;
    private String group;
    private MessageController messages;
    private CountDownLatch latch;

    public InitializeBaremetalCallable(String privateKey, String host, String loginUser, 
            StatementList initBootstrapScript, 
            String group, MessageController messages, CountDownLatch latch) {
        this.privateKey = privateKey;
        this.host = host;
        this.loginUser = loginUser;
        this.initScript = initBootstrapScript;
        this.group = group;
        this.messages = messages;
        this.latch = latch;
    }
    
    
    
    @Override
    public Set<NodeMetadata> call() throws Exception {
        Set<NodeMetadata> ready = new HashSet<NodeMetadata>();
                        client = new SSHClient();
                        client.addHostKeyVerifier(new PromiscuousVerifier());
                        try {
                            keys = client.loadKeys(privateKey, null, null);

                            client.connect(host);
                            client.authPublickey(loginUser, keys);
                            final Session session = client.startSession();
                            System.out.println("Starting SSH session on Host: "+host);
                            final Session.Command cmd = session.exec(initScript.render(OsFamily.UNIX));
                            System.out.println(IOUtils.readFully(cmd.getInputStream()).toString());
                            System.out.println("\n** exit status: " + cmd.getExitStatus());
                            session.close();
                            client.disconnect();
                            NodeMetadataBuilder builder = new NodeMetadataBuilder();
                            Set<String> publicIP = new HashSet<String>();
                            publicIP.add(host);
                            LoginCredentials credentials = LoginCredentials.builder().user(loginUser)
                                    .privateKey(privateKey).noPassword().build();
                            Location location = new LocationBuilder().id("KTHFS")
                                    .description("RandomRegion").scope(LocationScope.HOST).build();
                            NodeMetadata node = builder.hostname(host).id(host).location(location)
                                    .loginPort(22)
                                    .name("BareMetalNode").credentials(credentials).ids(host).providerId("Physical")
                                    .status(NodeMetadata.Status.RUNNING)
                                    .publicAddresses(publicIP).privateAddresses(publicIP).build();
                            ready.add(node);


                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.println("Error loading the private Key");
                        } finally {
                            messages.addMessage("Nodes created in Security Group " + group+ " with "
                                    + "basic setup");
                            latch.countDown();
                            return ready;
                        }
                    }
    }