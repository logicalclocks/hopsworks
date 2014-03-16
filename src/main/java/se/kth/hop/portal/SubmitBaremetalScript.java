package se.kth.hop.portal;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.scriptbuilder.domain.StatementList;

/**
 * Asynchronous Thread that handles SSH session to the node and executes the 
 * given script.
 * 
 *  It returns back the ExecResponse which wraps the result of executing the SSH session.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class SubmitBaremetalScript implements Callable<ExecResponse> {

    private String privateKey;
    private String host;
    private StatementList script;
    private SSHClient client;
    private KeyProvider keys;
    private String user;
    private CountDownLatch latch;

    public SubmitBaremetalScript(String privateKey, String host, StatementList script, String loginUser,
            CountDownLatch latch) {
        this.privateKey = privateKey;
        this.host = host;
        this.script = script;
        this.user = loginUser;
        this.latch = latch;
    }

    @Override
    public ExecResponse call() throws Exception {
        client = new SSHClient();
        client.addHostKeyVerifier(new PromiscuousVerifier());
        try {
            keys = client.loadKeys(privateKey, null, null);
            
            client.connect(host);
            client.authPublickey(user, keys);
            final Session session = client.startSession();

            System.out.println("Running Script on Host: " + host);
            final Session.Command cmd = session.exec(script.render(OsFamily.UNIX));

            String output = IOUtils.readFully(cmd.getInputStream()).toString();
            String error = cmd.getExitErrorMessage();
            int exitStatus = cmd.getExitStatus();

            System.out.println(output);
            System.out.println(error);
            System.out.println("\n** exit status: " + exitStatus);
            session.close();
            client.disconnect();
            ExecResponse response = new ExecResponse(output, error, exitStatus);
            latch.countDown();
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
