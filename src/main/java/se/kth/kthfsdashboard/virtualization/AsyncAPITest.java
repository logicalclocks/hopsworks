/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import se.kth.kthfsdashboard.provision.ScriptBuilder;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import java.net.URI;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.jclouds.ContextBuilder;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.openstack.nova.v2_0.NovaApi;
import org.jclouds.openstack.nova.v2_0.NovaAsyncApi;
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions;
import org.jclouds.openstack.nova.v2_0.domain.Server;
import org.jclouds.openstack.nova.v2_0.features.ServerApi;
import static com.google.common.collect.Iterables.getOnlyElement;
import org.jclouds.rest.RestContext;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.jclouds.sshj.config.SshjSshClientModule;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_PORT_OPEN;
import static org.jclouds.compute.config.ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE;
import static org.jclouds.Constants.PROPERTY_CONNECTION_TIMEOUT;
import static org.jclouds.compute.predicates.NodePredicates.TERMINATED;
import static org.jclouds.compute.predicates.NodePredicates.inGroup;
import static com.google.common.base.Predicates.not;
import com.google.common.collect.FluentIterable;
import static com.google.common.io.Closeables.closeQuietly;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.sun.istack.Pool;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadataBuilder;
import org.jclouds.openstack.nova.v2_0.domain.FloatingIP;
import org.jclouds.openstack.nova.v2_0.extensions.FloatingIPApi;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class AsyncAPITest {

    private ComputeService compute;
    private RestContext<NovaApi, NovaAsyncApi> nova;
    private static final URI RUBYGEMS_URI = URI.create("http://production.cf.rubygems.org/rubygems/rubygems-1.8.10.tgz");
    private String locationId = "RegionSICS";
    private static ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));

    public static void main(String[] args) {
        AsyncAPITest jCloudsNova = new AsyncAPITest();
        String group = "kthfs";

        try {
            jCloudsNova.init();

            //jCloudsNova.listServers();
            String publicKey = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDJwbktwUvTf1pKAL6TaGqZhD"
                    + "9sMQ0irG+9XHaKVVmg1Z970GYaojtgm/a56zQd1OCfoijVaVTLNJjNXAjbFSR8sg89+hSbf0S"
                    + "qhzWF5Xb99UbIDe48ZdhrKDqbCNIadUUMgbvbG7mziCtWUsDYP7rwskih4mVO09lK6ecXY"
                    + "c/RMsp09bnLf6TrM9JNJErQR0w8XsqI/TxMovjiBsOQH1TopdbrLRymVoBT/GGjgjy6Yy/yAJwqG"
                    + "c66ZsM1kIN0F32l5w8fiflesASGQyGxUutoCAgkpYY3DYlt7hdJXhTP/wuWCoI6zHWNCkYT8FG"
                    + "CPTVn650WZY9hR1Zn3Ycn4rlH a.lorenteleal@gmail.com";
            jCloudsNova.createTestInstance(group, publicKey);
            while(!pool.isTerminated()){
                    //System.out.println(System.currentTimeMillis());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jCloudsNova.close();
        }
    }

    private void init() {
        Iterable<Module> modules = ImmutableSet.<Module>of(
                new SshjSshClientModule(),
                new SLF4JLoggingModule(),
                new EnterpriseConfigurationModule());

        String provider = "openstack-nova";
        String identity = "SICS:albll"; // tenantName:userName
        String password = "alorlea@i7ibhaox"; // demo account uses ADMIN_PASSWORD too

        Properties properties = new Properties();
        long scriptTimeout = TimeUnit.MILLISECONDS.convert(50, TimeUnit.MINUTES);
        properties.setProperty(TIMEOUT_SCRIPT_COMPLETE, scriptTimeout + "");
        properties.setProperty(TIMEOUT_PORT_OPEN, scriptTimeout + "");
        properties.setProperty(PROPERTY_CONNECTION_TIMEOUT, scriptTimeout + "");
//        properties.setProperty(PROPERTY_PROXY_HOST, "127.0.0.1");
//        properties.setProperty(PROPERTY_PROXY_PORT, "2222");
//        properties.setProperty(PROPERTY_PROXY_SYSTEM, "true");

        ComputeServiceContext context = ContextBuilder.newBuilder(provider)
                .endpoint("http://193.10.64.166:5000/v2.0") //must be your keystone
                .credentials(identity, password)
                .modules(modules)
                .overrides(properties)
                .buildView(ComputeServiceContext.class);
        compute = context.getComputeService();
        nova = context.unwrap();

    }

    private void createTestInstance(String group, String key) {
        TemplateBuilder kthfsOpenstack = compute.templateBuilder();
        try {
            kthfsOpenstack.os64Bit(true);
            kthfsOpenstack.imageNameMatches("Ubuntu_12.04");

            kthfsOpenstack.hardwareId("RegionSICS/" + 4);
            ScriptBuilder initScript = ScriptBuilder.builder()
                    .scriptType(ScriptBuilder.ScriptType.INIT)
                    .publicKey(key)
                    .build();

            kthfsOpenstack.options(NovaTemplateOptions.Builder
                    //.inboundPorts(22, 80, 8080, 8181, 8686, 8983, 4848, 4040, 4000, 443)
                    .overrideLoginUser("ubuntu")
                    .generateKeyPair(true));
            //.userData(script.getBytes())
            //.authorizePublicKey(key)
            //.runScript(new StatementList(initScript)));
            NodeMetadata node = getOnlyElement(compute.createNodesInGroup(group, 1, kthfsOpenstack.build()));
            allocatePublicIP(compute, node);
//            compute.runScriptOnNodesMatching(
//                    Predicates.<NodeMetadata>and(not(TERMINATED), inGroup(group)), runChefSolo(),
//                    RunScriptOptions.Builder.nameTask("runchef-solo").overrideLoginCredentials(node.getCredentials()));
            final ListenableFuture<ExecResponse> future = compute.submitScriptOnNode(node.getId(), initScript, RunScriptOptions.NONE);
            future.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        final ExecResponse contents = future.get();
                        System.out.println("Finished launch of node!");
                        //...process 
                    } catch (InterruptedException e) {
                        System.out.println("Interrupted" + e);
                    } catch (ExecutionException e) {
                        System.out.println("Interrupted" + e.getCause());
                    }
                }
            }, pool);
        } catch (RunNodesException e) {
            System.err.println("error adding node to group " + group + ": " + e.getMessage());
        } catch (Exception e) {
            System.err.println("error: " + e.getMessage());
        }
    }

    /*
     * Script to run Chef Solo
     */
    private StatementList runChefSolo() {
        ImmutableList.Builder<Statement> bootstrapBuilder = ImmutableList.builder();
        //bootstrapBuilder.add(exec("sudo chef-solo -c /etc/chef/solo.rb -j http://lucan.sics.se/kthfs/chef.json -r http://lucan.sics.se/kthfs/kthfs-dash.tar.gz;"));
        return new StatementList(bootstrapBuilder.build());
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

    public void close() {
        closeQuietly(compute.getContext());
    }
}
