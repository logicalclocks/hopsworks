/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.deploy.provision;


import java.io.IOException;
import java.util.concurrent.Callable;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.jclouds.compute.domain.ExecResponse;


/**
 * Asynchronous Thread, the callback handles the SSH session and submits the script
 * to be retried on the node.
 * 
 * It returns back the ExecResponse which wraps the result of executing the SSH session.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class RetryNodeCallable implements Callable<ExecResponse> {

    private NodeProgression node;
    private String command;
    private SSHClient client;
    private KeyProvider keys;

    public RetryNodeCallable(NodeProgression node, String command) {
        this.node = node;
        this.command = command;
    }

    @Override
    public ExecResponse call() throws Exception {
        //Thread.sleep(10000);
        client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        try {
            keys = client.loadKeys(node.getPrivateKey(), null, null);
            String host= node.getIp();
            client.connect(host);
            client.authPublickey(node.getLoginUser(), keys);
            final Session session = client.startSession();
            
            System.out.println("Running Script on Host: "+host);
            final Command cmd = session.exec(command);

            String output = IOUtils.readFully(cmd.getInputStream()).toString();
            String error = cmd.getExitErrorMessage();
            int exitStatus = cmd.getExitStatus();
            
            System.out.println(output);
            System.out.println(error);
            System.out.println("\n** exit status: " + exitStatus);
            session.close();
            client.disconnect();
            ExecResponse response = new ExecResponse(output, error, exitStatus);
            return response;
        } catch (IOException e) {
            e.printStackTrace();
            //System.out.println("Error loading the private Key");
            ExecResponse response = new ExecResponse("Failed to execute the script",
                    "error submitting the script, sure we can connect?", 1);
            return response;
        } 
    }
}
