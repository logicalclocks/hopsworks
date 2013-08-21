/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.baremetal;

import java.io.IOException;
import java.util.concurrent.Callable;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.scriptbuilder.domain.OsFamily;
import se.kth.kthfsdashboard.provision.ScriptBuilder;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class SubmitScriptBaremetalCallable implements Callable<ExecResponse> {

    private NodeMetadata node;
    private ScriptBuilder script;
    private SSHClient client;
    private KeyProvider keys;

    public SubmitScriptBaremetalCallable(NodeMetadata node, ScriptBuilder script) {
        this.node = node;
        this.script = script;
    }

    @Override
    public ExecResponse call() throws Exception {
        client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        try {
            keys = client.loadKeys(node.getCredentials().getPrivateKey(), null, null);
            String host= node.getPrivateAddresses().iterator().next();
            client.connect(host);
            client.authPublickey(node.getCredentials().getUser(), keys);
            final Session session = client.startSession();
            
            System.out.println("Running Script on Host: "+host);
            final Command cmd = session.exec(script.render(OsFamily.UNIX));

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
            System.out.println("Error loading the private Key");
        } finally {
            ExecResponse response = new ExecResponse("Failed to execute the script",
                    "error submitting the script, sure we can connect?", 1);
            return response;
        }
    }
}
