/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.jclouds.chef.util.RunListBuilder;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import static org.jclouds.scriptbuilder.domain.Statements.createOrOverwriteFile;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import org.jclouds.scriptbuilder.statements.ruby.InstallRubyGems;
import org.jclouds.scriptbuilder.statements.ssh.AuthorizeRSAPublicKeys;

/**
 * Setups the script to run on the VM node. For now it has the init script and
 * the generic script for the nodes.
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class ScriptBuilder implements Statement {

    public static enum ScriptType {

        INIT, INSTALL, JHDFS, INSTALLBAREMETAL,CONFIGBAREMETAL
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private ScriptType scriptType;
        private List<String> ndbs;
        private List<String> mgms;
        private List<String> mysql;
        private List<String> namenodes;
        private List<String> roles;
        private String nodeIP;
        private String key;
        private String privateIP;
        private String clusterName;
        private String nodeId;

        /*
         * Define the type of script we are going to prepare
         */
        public Builder scriptType(ScriptType type) {
            this.scriptType = type;
            return this;
        }

        /*
         * JHDFS Script
         * List of ndbs for chef to configure the nodes
         */
        public Builder ndbs(List<String> ndbs) {
            this.ndbs = ndbs;
            return this;
        }

        /*
         * JHDFS Script
         * List of mgms for chef to configure the nodes
         */
        public Builder mgms(List<String> mgms) {
            this.mgms = mgms;
            return this;
        }

        /*
         * JHDFS Script
         * List of mysql for chef to configure the nodes
         */
        public Builder mysql(List<String> mysql) {
            this.mysql = mysql;
            return this;
        }

        /*
         * JHDFS Script
         * List of namenodes for chef to configure the nodes
         */
        public Builder namenodes(List<String> namenodes) {
            this.namenodes = namenodes;
            return this;
        }

        /*
         * JHDFS Script
         * List of roles for chef to configure the nodes
         */
        public Builder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        /*
         * JHDFS Script
         * IP of the node
         */
        public Builder nodeIP(String ip) {
            this.nodeIP = ip;
            return this;
        }
        /*
         * INIT Script
         * public key to authorize
         */

        public Builder publicKey(String key) {
            this.key = key;
            return this;
        }

        public Builder privateIP(String ip) {
            this.privateIP = ip;
            return this;
        }

        public Builder clusterName(String name) {
            this.clusterName = name;
            return this;
        }

        public Builder nodeId(String id) {
            this.nodeId = id;
            return this;
        }
        /*
         * Default script build, use when defined all the other building options
         */

        public ScriptBuilder build() {
            return new ScriptBuilder(scriptType, ndbs, mgms, mysql,
                    namenodes, roles, nodeIP, nodeId, key, privateIP, clusterName);
        }
        /*
         * Same as default but in this case we include the ip during the build and the roles.
         */

        public ScriptBuilder build(String ip, List<String> roles, String nodeId) {
            return new ScriptBuilder(scriptType, ndbs, mgms, mysql,
                    namenodes, roles, ip, nodeId, key, privateIP, clusterName);
        }
    }
    private ScriptType scriptType;
    private List<String> ndbs;
    private List<String> mgms;
    private List<String> mysql;
    private List<String> namenodes;
    private List<String> roles;
    private String key;
    private String ip;
    private String privateIP;
    private String clusterName;
    private String nodeId;

    protected ScriptBuilder(ScriptType scriptType, List<String> ndbs, List<String> mgms,
            List<String> mysql, List<String> namenodes, List<String> roles, String ip, String nodeId, String key,
            String privateIP, String clusterName) {
        this.scriptType = scriptType;
        this.ndbs = ndbs;
        this.mgms = mgms;
        this.mysql = mysql;
        this.namenodes = namenodes;
        this.roles = roles;
        this.ip = ip;
        this.key = key;
        this.privateIP = privateIP;
        this.clusterName = clusterName;
        this.nodeId = nodeId;
    }

    @Override
    public Iterable<String> functionDependencies(OsFamily family) {
        return ImmutableSet.<String>of();
    }

    @Override
    public String render(OsFamily family) {
        if (family == OsFamily.WINDOWS) {
            throw new UnsupportedOperationException("windows not yet implemented");
        }

        ImmutableList.Builder<Statement> statements = ImmutableList.builder();
        switch (scriptType) {
            case INIT:
                statements.add(exec("sudo dpkg --configure -a"));
                statements.add(exec("sudo apt-get update;"));
                List<String> keys = new ArrayList();
                keys.add(key);
                statements.add(new AuthorizeRSAPublicKeys(keys));
                statements.add(exec("sudo apt-get install -f -y --force-yes make;"));
                statements.add(exec("sudo apt-get install -f -y -qq --force-yes ruby1.9.1-full;"));
                statements.add(exec("sudo apt-get install -f -y --force-yes git;"));
                statements.add(InstallRubyGems.builder()
                        .version("1.8.10")
                        .build());
//                statements.add(
//                        InstallChefGems.builder()
//                        .version("10.20.0").build());
                statements.add(exec("sudo gem install chef -v '10.20.0' --no-rdoc"));

                statements.add(exec("sudo mkdir /etc/chef;"));
                statements.add(exec("cd /etc/chef;"));
                statements.add(exec("sudo wget http://lucan.sics.se/kthfs/solo.rb;"));
                //Setup and fetch git recipes
                statements.add(exec("git config --global user.name \"Jim Dowling\";"));
                statements.add(exec("git config --global user.email \"jdowling@sics.se\";"));
                statements.add(exec("git config --global http.sslVerify false;"));
                statements.add(exec("git config --global http.postBuffer 524288000;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone https://ghetto.sics.se/jdowling/kthfs-pantry.git /tmp/chef-solo/;"));
                statements.add(exec("sudo apt-get install -f -q -y libmysqlclient-dev;"));

                break;

            case INSTALL:
                createNodeInstall(statements);
                statements.add(exec("sudo chef-solo -c /etc/chef/solo.rb -j /etc/chef/chef.json"));
                break;
            case JHDFS:
                createNodeConfiguration(statements);
                statements.add(exec("sudo chef-solo -c /etc/chef/solo.rb -j /etc/chef/chef.json"));
                break;
            case INSTALLBAREMETAL:
                createBaremetalInstall(statements);
                statements.add(exec("sudo chef-solo -c /etc/chef/solo.rb -j /etc/chef/chef.json"));
                break;
            case CONFIGBAREMETAL:
                createBaremetalConfig(statements);
                statements.add(exec("sudo chef-solo -c /etc/chef/solo.rb -j /etc/chef/chef.json"));
                break;

        }

        return new StatementList(statements.build()).render(family);

    }

    /*
     * Here we generate the json file and the runlists we need for chef in the nodes
     * We need the ndbs, mgms, mysqlds and namenodes ips.
     * Also we need to know the security group to generate the runlist of recipes for that group based on 
     * the roles and the node metadata to get its ips.
     */
    private void createNodeConfiguration(ImmutableList.Builder<Statement> statements) {
        //First we generate the recipe runlist based on the roles defined in the security group of the cluster
        List<String> runlist = createRunList();
        //Start json
        StringBuilder json = generateConfigJSON(runlist);
        //Create the file in this directory in the node
        statements.add(createOrOverwriteFile("/etc/chef/chef.json", ImmutableSet.of(json.toString())));
    }

    private void createNodeInstall(ImmutableList.Builder<Statement> statements) {
        List<String> installList = createInstallList();
        //Start json
        StringBuilder json = generateInstallJSON(installList);

        statements.add(createOrOverwriteFile("/etc/chef/chef.json", ImmutableSet.of(json.toString())));
    }

    /*
     * Baremetal scripts
     */
    private void createBaremetalInstall(ImmutableList.Builder<Statement> statements) {
        List<String> installList = createInstallList();
        //Start json
        StringBuilder json = generateInstallJSON(installList);
        statements.add(exec("sudo bash -c 'cat > /etc/chef/chef.json <<-'END_OF_FILE'\n"
                + json.toString() + "\nEND_OF_FILE';"));
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
    
    /*
     * JSON generators for install list and config lists
     */

    private StringBuilder generateInstallJSON(List<String> installList) {
        //Start json
        StringBuilder json = new StringBuilder();
        //Open json bracket
        json.append("{");
        //Recipe runlist append in the json
        json.append("\"run_list\":[");
        for (int i = 0; i < installList.size(); i++) {
            if (i == installList.size() - 1) {
                json.append("\"").append(installList.get(i)).append("\"");
            } else {
                json.append("\"").append(installList.get(i)).append("\",");
            }
        }
        //close the json
        json.append("]}");
        return json;
    }

    private StringBuilder generateConfigJSON(List<String> runlist) {
        //Start json
        StringBuilder json = new StringBuilder();
        //Open json bracket
        json.append("{");
        //First generate the ndb fragment
        // JIM: Note there can be multiple mgm servers, not just one.
        json.append("\"ndb\":{  \"mgm_server\":{\"addrs\": [");

        //Iterate mgm servers and add them.

        for (int i = 0; i < mgms.size(); i++) {
            if (i == mgms.size() - 1) {
                json.append("\"").append(mgms.get(i)).append("\"");
            } else {
                json.append("\"").append(mgms.get(i)).append("\",");
            }
        }
        json.append("]},");
        //Iterate ndbds addresses
        json.append("\"ndbd\":{\"addrs\":[");
        for (int i = 0; i < ndbs.size(); i++) {
            if (i == ndbs.size() - 1) {
                json.append("\"").append(ndbs.get(i)).append("\"");
            } else {
                json.append("\"").append(ndbs.get(i)).append("\",");
            }
        }
        json.append("]},");
        //Get the mgms ips and add to the end the ips of the mysqlds
        List<String> ndapi = new LinkedList(mgms);
        ndapi.addAll(mysql);
        //Generate ndbapi with ndapi ips
        json.append("\"ndbapi\":{\"addrs\":[");
        for (int i = 0; i < ndapi.size(); i++) {
            if (i == ndapi.size() - 1) {
                json.append("\"").append(ndapi.get(i)).append("\"");
            } else {
                json.append("\"").append(ndapi.get(i)).append("\",");
            }
        }
        json.append("]},");
        //Get the nodes private ip
        //List<String> ips = new LinkedList(data.getPrivateAddresses());
        //add the ip in the json
        json.append("\"ip\":\"").append(ip).append("\",");
        //***
        json.append("\"data_memory\":\"120\",");

        //Generate name of cluster and service for MYSQL
        json.append("\"cluster\":\"").append(clusterName).append("\",");

        json.append("\"num_ndb_slots_per_client\":\"2\"},");
        json.append("\"memcached\":{\"mem_size\":\"128\"},");
        //***
        //Generate collectd fragment
        json.append("\"collectd\":{\"server\":\"").append(privateIP).append("\",");
        json.append("\"clients\":[");
        //Depending of the security group name of the demo we specify which collectd config to use
        Set<String> roleSet = new HashSet<String>(roles);
        if (roleSet.contains("MySQLCluster-mysqld") // JIM: We can just have an empty clients list for mgm and ndb nodes    
                //                || group.getSecurityGroup().equals("mgm")
                //                || group.getSecurityGroup().equals("ndb")
                ) {
            json.append("\"mysqld\"");
        }
        if (roleSet.contains("KTHFS-datanode")) {
            json.append("\"dn\"");
        }
        if (roleSet.contains("KTHFS-namenode")) {
            json.append("\"nn\"");
        }
        json.append("]},");
        //Generate kthfs fragment
        //server ip of the dashboard
        json.append("\"kthfs\":{\"server_ip\":\"").append(privateIP).append("\",");
        //mgm ip
        //TODO ADD SUPPORT FOR MULTIPLE MGMS
        json.append("\"ndb_connectstring\":\"").append(mgms.get(0)).append("\",");
        //namenodes ips

        json.append("\"cluster\":\"").append(clusterName).append("\",");
        json.append("\"hostid\":\"").append(nodeId).append("\",");

        if (roleSet.contains("KTHFS-datanode") || roleSet.contains("KTHFS-namenode")) {
            json.append("\"service\":\"").append("KTHFS").append("\",");
        }
//        else{
//            json.append("\"service\":\"").append("MySQLCluster").append("\",");
//        }

        json.append("\"namenode\":{\"addrs\":[");

        for (int i = 0; i < namenodes.size(); i++) {
            if (i == namenodes.size() - 1) {
                json.append("\"").append(namenodes.get(i)).append("\"");
            } else {
                json.append("\"").append(namenodes.get(i)).append("\",");
            }
        }
        json.append("]},");
        //My own ip
        json.append("\"ip\":\"").append(ip).append("\"");
        json.append("},");
        //Recipe runlist append in the json
        json.append("\"run_list\":[");
        for (int i = 0; i < runlist.size(); i++) {
            if (i == runlist.size() - 1) {
                json.append("\"").append(runlist.get(i)).append("\"");
            } else {
                json.append("\"").append(runlist.get(i)).append("\",");
            }
        }
        //close the json
        json.append("]}");
        return json;
    }

    /*
     * Chef runlist generation
     */
    private List<String> createInstallList() {
        RunListBuilder builder = new RunListBuilder();
        builder.addRecipe("ndb::install");
        builder.addRecipe("java");
        builder.addRecipe("kthfs::install");
        return builder.build();
    }

    private List<String> createRunList() {
        RunListBuilder builder = new RunListBuilder();
        // builder.addRecipe("kthfsagent");
        builder.addRecipe("kthfsagent");
        boolean collectdAdded = false;
        //Look at the roles, if it matches add the recipes for that role
        for (String role : roles) {
            if (role.equals("MySQLCluster-ndb")) {

                builder.addRecipe("ndb::ndbd");
                builder.addRecipe("ndb::ndbd-kthfs");
                collectdAdded = true;
            }
            if (role.equals("MySQLCluster-mysqld")) {

                builder.addRecipe("ndb::mysqld");
                builder.addRecipe("ndb::mysqld-kthfs");
                collectdAdded = true;
            }
            if (role.equals("MySQLCluster-mgm")) {

                builder.addRecipe("ndb::mgmd");
                builder.addRecipe("ndb::mgmd-kthfs");
                collectdAdded = true;
            }
            if (role.equals("MySQLCluster-memcached")) {
                builder.addRecipe("ndb::memcached");
                builder.addRecipe("ndb::memcached-kthfs");
            }

            //This are for the Hadoop nodes
            if (role.equals("KTHFS-namenode")) {
                builder.addRecipe("java");
                builder.addRecipe("kthfs::namenode");
                collectdAdded = true;
            }
            if (role.equals("KTHFS-datanode")) {
                builder.addRecipe("java");
                builder.addRecipe("kthfs::datanode");
                collectdAdded = true;
            }
            if (collectdAdded) {
                //builder.addRecipe("kthfsagent");
                builder.addRecipe("collect::attr-driven");
            }
            // We always need to restart the kthfsagent after we have
            // updated its list of services
            builder.addRecipe("java::openjdk");
            builder.addRecipe("kthfsagent::restart");

        }

        return builder.build();


    }
}
