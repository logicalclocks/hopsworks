/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.portal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jclouds.compute.domain.ExecResponse;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import static org.jclouds.scriptbuilder.domain.Statements.createOrOverwriteFile;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import org.jclouds.scriptbuilder.statements.ssh.AuthorizeRSAPublicKeys;
import se.kth.kthfsdashboard.provision.MessageController;

/**
 * Represents a provisioning process for a baremetal machine in order to install the 
 * dashboard.
 * Contains methods that represent this process and generate the scripts to configure the dashboard.
 * 
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class BaremetalProvision {

    private String host;
    private String user;
    private String privateKey;
    private String publicKey;
    private boolean publicKeyEnabled;
    private MessageController messages;

    public BaremetalProvision(PortalMB provider, MessageController messages) {
        this.host = provider.getHost();
        this.user = provider.getLoginUser();
        this.privateKey = provider.getPrivateKey();
        this.messages = messages;
        this.publicKey = provider.getPublicKey();
        this.publicKeyEnabled = provider.isPublicKeyEnabled();
    }

    public void configureBaremetal() {
        //Initialize the node we want
        CountDownLatch latch = new CountDownLatch(1);
        ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(1));
        messages.addMessage("Configuring SSH for " + host);
        ListenableFuture<Set<NodeMetadata>> groupCreation =
                pool.submit(new InitializeBaremetalCallable(privateKey, host, user, initBootstrapScript(),
                host, messages, latch));
        try {
            latch.await(30, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            System.out.println("Failed to launch init script in some machines");

        }

        //Now we run chef in the node;
        System.out.println("Launching the second phase of the script");
        latch = new CountDownLatch(1);
        messages.addMessage("Running chef-solo, SSH for " + host);
        ListenableFuture<ExecResponse> future = pool.submit(
                new SubmitBaremetalScript(privateKey, host, runChefSolo(), user, latch));
        try {
            latch.await(30, TimeUnit.MINUTES);

        } catch (InterruptedException e) {
            System.out.println("Failed to launch chef script in some machines");

        }
        messages.addSuccessMessage("http://" + host + ":8080/hop-dashboard, Private IP: " + host);
        messages.setDashboardURL("http://" + host + ":8080/hop-dashboard");
        messages.setDashboardPrivateIP(host);
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
        bootstrapBuilder.add(exec("sudo apt-get install -f -y --force-yes git;"));
        if (publicKeyEnabled) {
            List<String> keys = new ArrayList();
            keys.add(publicKey);
            bootstrapBuilder.add(new AuthorizeRSAPublicKeys(keys));
        }
        bootstrapBuilder.add(exec("curl -L https://www.opscode.com/chef/install.sh | sudo bash"));

        bootstrapBuilder.add(exec("sudo mkdir /etc/chef;"));
        bootstrapBuilder.add(exec("cd /etc/chef;"));

        List<String> soloLines = new ArrayList<String>();
        soloLines.add("file_cache_path \"/tmp/chef-solo\"");
        soloLines.add("cookbook_path \"/tmp/chef-solo/cookbooks\"");
        bootstrapBuilder.add(createOrOverwriteFile("/etc/chef/solo.rb", soloLines));

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
    private StatementList runChefSolo() {
        ImmutableList.Builder<Statement> bootstrapBuilder = ImmutableList.builder();
        createBaremetalConfig(bootstrapBuilder);
        bootstrapBuilder.add(exec("sudo chef-solo -c /etc/chef/solo.rb -j /etc/chef/chef.json"));
        return new StatementList(bootstrapBuilder.build());
    }

    private void createBaremetalConfig(ImmutableList.Builder<Statement> statements) {
        //First we generate the recipe runlist based on the roles defined in the security group of the cluster
        List<String> runlist = createRunList();
        //Start json
        StringBuilder json = generateConfigJSON(runlist);
        //Create the file in this directory in the node
        statements.add(exec("sudo bash -c 'cat > /etc/chef/chef.json <<-'END_OF_FILE'\n"
                + json.toString() + "\nEND_OF_FILE'"));
    }

    private StringBuilder generateConfigJSON(List<String> runList) {
        //Start json
        StringBuilder json = new StringBuilder();
        //Open json bracket
        json.append("{");
        //mysql
        json.append("\"mysql\":{\"user\":\"root\",\"user_password\":\"kthfs\","
                + "\"server_debian_password\":\"kthfs\",\"server_root_password\":\"kthfs\","
                + "\"server_repl_password\":\"kthfs\",\"bind_address\":\"localhost\""
                + "},");
         //Paascredentials
       json.append("\"dashboard\":{\"private_ip\":\"")
                .append(host)
                .append("\",\"public_key\":\"")
                .append(publicKey)
                .append("\"},");
        json.append("\"provider\":{\"name\":\"")
                .append("baremetal")
                .append("\",\"access_key\":\"")
                .append("")
                .append("\",\"account_id\":\"")
                .append("")
                .append("\",\"keystone_url\":\"")
                .append("")
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

    private void createNodeConfiguration(ImmutableList.Builder<Statement> statements) {
        //First we generate the recipe runlist based on the roles defined in the security group of the cluster
        List<String> runlist = createRunList();
        //Start json
        StringBuilder json = generateConfigJSON(runlist);
        //Create the file in this directory in the node
        statements.add(exec("sudo apt-get clean;"));
        statements.add(exec("sudo apt-get update;"));
        statements.add(createOrOverwriteFile("/etc/chef/chef.json", ImmutableSet.of(json.toString())));
    }

    private List<String> createRunList() {
        List<String> list = new ArrayList<String>();
        list.add("authbind");
        list.add("java");
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
