/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import se.kth.kthfsdashboard.provision.ScriptBuilder;
import com.google.common.base.Function;
import com.google.common.eventbus.EventBus;
import com.google.common.io.Files;
import com.google.common.net.HostAndPort;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import org.jclouds.compute.callables.RunScriptOnNodeUsingSsh;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.domain.Location;
import org.jclouds.domain.LocationBuilder;
import org.jclouds.domain.LocationScope;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.http.handlers.BackoffLimitedRetryHandler;
import org.jclouds.javax.annotation.Nullable;
import org.jclouds.ssh.SshClient;
import org.jclouds.sshj.SshjSshClient;
import static com.google.inject.name.Names.bindProperties;
import java.io.File;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.sshj.config.SshjSshClientModule;
import static com.google.common.base.Charsets.UTF_8;
import java.io.IOException;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class SSHClientTestJclouds {

    EventBus eventBus = new EventBus();
    private SshClient sshClient;
    private NodeMetadata node;
    private Function<NodeMetadata, SshClient> sshFactory;
    String publicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDJwbktwUvTf1pKAL6TaGqZhD"
            + "9sMQ0irG+9XHaKVVmg1Z970GYaojtgm/a56zQd1OCfoijVaVTLNJjNXAjbFSR8sg89+hSbf0S"
            + "qhzWF5Xb99UbIDe48ZdhrKDqbCNIadUUMgbvbG7mziCtWUsDYP7rwskih4mVO09lK6ecXY"
            + "c/RMsp09bnLf6TrM9JNJErQR0w8XsqI/TxMovjiBsOQH1TopdbrLRymVoBT/GGjgjy6Yy/yAJwqG"
            + "c66ZsM1kIN0F32l5w8fiflesASGQyGxUutoCAgkpYY3DYlt7hdJXhTP/wuWCoI6zHWNCkYT8FG"
            + "CPTVn650WZY9hR1Zn3Ycn4rlH a.lorenteleal@gmail.com";

    public static void main(String[] args) {

        SSHClientTestJclouds test = new SSHClientTestJclouds();
//        test.createClient(new Properties());
        test.init();
        test.simpleTest();
    }

    public void init() {

        String privateKey = "-----BEGIN RSA PRIVATE KEY-----"
                + "MIICXgIBAAKBgQDkRFkyQ8yRzE9WjXnjjaroCznG72CUALvqsCFS4UcuVx+KB2xK"
                + "FpnTqalUaTChWjsK6mOTjb2qzOqHJDsNZO8o5Ev/l/4BORbUFXf/GQlvwT9AIRL1"
                + "qVGwRRLSRcmyJtNrX5K0iKprFY9YP4NrV+ZmoEBJFwmb9Eb/Ekpbv4p3QwIDAQAB"
                + "AoGALE+gSQOkSIEyvYiFKskrbhQPyTBavSBPWkWKkn4sxTAgbTj3qoIspkv/FOW+"
                + "jPPpFjtdzRzsvqU8ubMsy3LWgenC7znam3L9GpcMq4agDbmt8ybb67V3oCPi4HTQ"
                + "T8EfmUQGJdWFuQcMp7m6lb+wmG+xAWwmMmbYGaoeXrKEhqECQQD0+p3sg5lki0QI"
                + "2KiTA/ye1ce/fTKteBNKcLAOu4D3fqpGU/+y7mJ9wVwMvACbFARgeSzS6BnhQ+x9"
                + "u0bCPlcrAkEA7olDJehUUcb5IGXQ4hxh6jIoE13uGUhgQj/ZDjDpj0nrpTrOMi/O"
                + "3aLyiCIE/fjQwb2rrUyj0a8SDX7EKLzUSQJBAJcxIUw5/+50oP2QsaFiQYPJzqiY"
                + "3TEAPW+g0peVE0gr7WzQJKxKwZB5SJU3ZmxPU1AzGP3lbyt+3zLN5SK2lNcCQQCk"
                + "nCC0hjG6BV9iViDiCMghP9+cDdQDqoiS71CwlFx5P3/YlE47H/bXyF0qSJ+9S/lz"
                + "2Zohi6P5TaFdor9nhXfRAkEA3qQo97plSYK935dGhyVS16c+aCptJD0xWLrYWXnR"
                + "Ol6Nod4yjHi+dvcOuaFLBnl3eX2m8TXbkEnOEaYan3O56Q==\n"
                + "-----END RSA PRIVATE KEY-----";

        try{
        privateKey = Files.toString(
                new File(System.getProperty("user.home") + "/Downloads/hortonnodes.pem"), UTF_8);
        }
        catch(IOException e){
            System.out.println("dafuq!");
        }
        
        LoginCredentials credentials = LoginCredentials.builder().user("ubuntu").privateKey(privateKey).noPassword().build();
        HostAndPort host = HostAndPort.fromParts("193.10.64.127", 22);
        sshClient = new SshjSshClient(BackoffLimitedRetryHandler.INSTANCE, host, credentials, 30000);

        sshFactory = new Function<NodeMetadata, SshClient>() {
            @Override
            public SshClient apply(@Nullable NodeMetadata nodeMetadata) {
                return sshClient;
            }
        };
        NodeMetadataBuilder builder = new NodeMetadataBuilder();
        Location location = new LocationBuilder().id("KTHFS").description("RandomRegion").scope(LocationScope.HOST).build();
        Set<String> publicIP = new HashSet<String>();
        publicIP.add("193.10.64.127");

        Set<String> privateIP = new HashSet<String>(publicIP);
        node = builder.hostname("193.10.64.127").id("KTHFS").location(location).loginPort(22)
                .name("BareMetalNode").credentials(credentials).ids("KTHFS").providerId("Physical")
                .status(NodeMetadata.Status.RUNNING)
                .publicAddresses(publicIP).privateAddresses(privateIP).build();
    }

    public void simpleTest() {
        ScriptBuilder initScript = ScriptBuilder.builder()
                .scriptType(ScriptBuilder.ScriptType.INIT)
                .publicKey(publicKey)
                .build();

        RunScriptOnNodeUsingSsh testMe = new RunScriptOnNodeUsingSsh(sshFactory, eventBus, node, initScript, RunScriptOptions.NONE);
        testMe.init();
        sshClient.connect();
        sshClient.exec("sudo sh <<'RUN_SCRIPT_AS_ROOT_SSH'\n" 
                + "RUN_SCRIPT_AS_ROOT_SSH\n");
        sshClient.disconnect();
        testMe.call();


    }

    protected SshjSshClient createClient(final Properties props) {

        String privateKey = "-----BEGIN RSA PRIVATE KEY-----"
                + "MIICXgIBAAKBgQDkRFkyQ8yRzE9WjXnjjaroCznG72CUALvqsCFS4UcuVx+KB2xK"
                + "FpnTqalUaTChWjsK6mOTjb2qzOqHJDsNZO8o5Ev/l/4BORbUFXf/GQlvwT9AIRL1"
                + "qVGwRRLSRcmyJtNrX5K0iKprFY9YP4NrV+ZmoEBJFwmb9Eb/Ekpbv4p3QwIDAQAB"
                + "AoGALE+gSQOkSIEyvYiFKskrbhQPyTBavSBPWkWKkn4sxTAgbTj3qoIspkv/FOW+"
                + "jPPpFjtdzRzsvqU8ubMsy3LWgenC7znam3L9GpcMq4agDbmt8ybb67V3oCPi4HTQ"
                + "T8EfmUQGJdWFuQcMp7m6lb+wmG+xAWwmMmbYGaoeXrKEhqECQQD0+p3sg5lki0QI"
                + "2KiTA/ye1ce/fTKteBNKcLAOu4D3fqpGU/+y7mJ9wVwMvACbFARgeSzS6BnhQ+x9"
                + "u0bCPlcrAkEA7olDJehUUcb5IGXQ4hxh6jIoE13uGUhgQj/ZDjDpj0nrpTrOMi/O"
                + "3aLyiCIE/fjQwb2rrUyj0a8SDX7EKLzUSQJBAJcxIUw5/+50oP2QsaFiQYPJzqiY"
                + "3TEAPW+g0peVE0gr7WzQJKxKwZB5SJU3ZmxPU1AzGP3lbyt+3zLN5SK2lNcCQQCk"
                + "nCC0hjG6BV9iViDiCMghP9+cDdQDqoiS71CwlFx5P3/YlE47H/bXyF0qSJ+9S/lz"
                + "2Zohi6P5TaFdor9nhXfRAkEA3qQo97plSYK935dGhyVS16c+aCptJD0xWLrYWXnR"
                + "Ol6Nod4yjHi+dvcOuaFLBnl3eX2m8TXbkEnOEaYan3O56Q==\n"
                + "-----END RSA PRIVATE KEY-----";
        try{
        privateKey = Files.toString(
                new File(System.getProperty("user.home") + "/Downloads/hortonnodes.pem"), UTF_8);
        }
        catch(IOException e){
            System.out.println("dafuq!");
        }
        LoginCredentials credentials = LoginCredentials.builder().user("ubuntu").privateKey(privateKey).noPassword().build();
        
        
        HostAndPort host = HostAndPort.fromParts("193.10.64.127", 22);
        Injector i = Guice.createInjector(module(), new AbstractModule() {
            @Override
            protected void configure() {
                bindProperties(binder(), props);
            }
        }, new SLF4JLoggingModule());
        SshClient.Factory factory = i.getInstance(SshClient.Factory.class);
        SshjSshClient ssh = SshjSshClient.class.cast(factory.create(host, credentials));
        sshClient = ssh;
        return ssh;
    }

    protected Module module() {
        return new SshjSshClientModule();
    }
}
