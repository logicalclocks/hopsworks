/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.jclouds.scriptbuilder.domain.OsFamily;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import static org.jclouds.scriptbuilder.domain.Statements.createOrOverwriteFile;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import org.jclouds.scriptbuilder.domain.chef.RunList;
import org.jclouds.scriptbuilder.statements.ssh.AuthorizeRSAPublicKeys;

/**
 * Setups the script to run on the VM node. 
 * It has multiple options when generating a script depending of the parameters
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class ScriptBuilder implements Statement {

    public static enum ScriptType {

        INIT, INSTALL, HOPS, INSTALLBAREMETAL, CONFIGBAREMETAL, RECOVER
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
        private String gitName;
        private String gitKey;
        private String gitRepo;

        /*
         * Define the type of script we are going to prepare
         */
        public Builder scriptType(ScriptType type) {
            this.scriptType = type;
            return this;
        }

        /*
         * HOPS Script
         * List of ndbs for chef to configure the nodes
         */
        public Builder ndbs(List<String> ndbs) {
            this.ndbs = ndbs;
            return this;
        }

        /*
         * HOPS Script
         * List of mgms for chef to configure the nodes
         */
        public Builder mgms(List<String> mgms) {
            this.mgms = mgms;
            return this;
        }

        /*
         * HOPS Script
         * List of mysql for chef to configure the nodes
         */
        public Builder mysql(List<String> mysql) {
            this.mysql = mysql;
            return this;
        }

        /*
         * HOPS Script
         * List of namenodes for chef to configure the nodes
         */
        public Builder namenodes(List<String> namenodes) {
            this.namenodes = namenodes;
            return this;
        }

        /*
         * HOPS Script
         * List of roles for chef to configure the nodes
         */
        public Builder roles(List<String> roles) {
            this.roles = roles;
            return this;
        }

        /*
         * HOPS Script
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

        public Builder gitName(String name) {
            this.gitName = name;
            return this;
        }

        public Builder gitKey(String key) {
            this.gitKey = key;
            return this;
        }

        public Builder gitRepo(String repo) {
            this.gitRepo = repo;
            return this;
        }

        /*
         * Default script build, use when defined all the other building options
         */
        public ScriptBuilder build() {
            return new ScriptBuilder(scriptType, ndbs, mgms, mysql,
                    namenodes, roles, nodeIP, nodeId, key, privateIP, clusterName, gitName, gitKey, gitRepo);
        }
        /*
         * Same as default but in this case we include the ip during the build and the roles.
         */

        public ScriptBuilder build(String ip, List<String> roles, String nodeId) {
            return new ScriptBuilder(scriptType, ndbs, mgms, mysql,
                    namenodes, roles, ip, nodeId, key, privateIP, clusterName, gitName, gitKey, gitRepo);
        }

        @Override
        public String toString() {
            return "Builder{" + "scriptType=" + scriptType + ", ndbs=" + ndbs + ", mgms=" + mgms
                    + ", mysql=" + mysql + ", namenodes=" + namenodes + ", roles=" + roles + ", nodeIP="
                    + nodeIP + ", key=" + key + ", privateIP=" + privateIP + ", clusterName=" + clusterName
                    + ", nodeId=" + nodeId + ", gitName=" + gitName + ", gitKey=" + gitKey + ", gitRepo="
                    + gitRepo + '}';
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
    private String gitName;
    private String gitKey;
    private String gitRepo;
    private HashSet<String> filterClients = new HashSet<String>();

    protected ScriptBuilder(ScriptType scriptType, List<String> ndbs, List<String> mgms,
            List<String> mysql, List<String> namenodes, List<String> roles, String ip, String nodeId, String key,
            String privateIP, String clusterName, String gitName, String gitKey, String gitRepo) {
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
        this.gitName = (gitName == null || gitName.equals("")) ? "Jim Dowling" : gitName;
        this.gitKey = gitKey; //To do the same
        this.gitRepo = (gitRepo == null || gitRepo.equals(""))
                ? "https://github.com/hopstart/hop-chef.git" : gitRepo;
        filterClients.add("ndb::mysqld");
        filterClients.add("hop::datanode");
        filterClients.add("hop::namenode");
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

                statements.add(exec("sudo apt-get update;"));
                List<String> keys = new ArrayList();
                keys.add(key);
                statements.add(new AuthorizeRSAPublicKeys(keys));
                statements.add(exec("sudo apt-get install -f -y --force-yes make;"));
                statements.add(exec("sudo apt-get install -f -y -qq --force-yes ruby1.9.1-full;"));
                statements.add(exec("sudo apt-get install -f -y --force-yes git;"));
                statements.add(exec("curl -L https://www.opscode.com/chef/install.sh | sudo bash"));

                statements.add(exec("sudo mkdir /etc/chef;"));
                statements.add(exec("cd /etc/chef;"));
                List<String> soloLines = new ArrayList<String>();
                soloLines.add("file_cache_path \"/tmp/chef-solo\"");
                soloLines.add("cookbook_path \"/tmp/chef-solo/cookbooks\"");
                statements.add(createOrOverwriteFile("/etc/chef/solo.rb", soloLines));
                //Setup and fetch git recipes
                statements.add(exec("git config --global user.name \"" + gitName + "\";"));

                statements.add(exec("git config --global http.sslVerify false;"));
                statements.add(exec("git config --global http.postBuffer 524288000;"));
                statements.add(exec("sudo git clone " + gitRepo + " /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone " + gitRepo + " /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone " + gitRepo + " /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone " + gitRepo + " /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone " + gitRepo + " /tmp/chef-solo/;"));
                statements.add(exec("sudo git clone " + gitRepo + " /tmp/chef-solo/;"));
                statements.add(exec("sudo apt-get install -f -q -y libmysqlclient-dev;"));

                break;

            case INSTALL:
                createNodeInstall(statements);
                statements.add(exec("sudo chef-solo -c /etc/chef/solo.rb -j /etc/chef/chef.json"));
                break;
            case HOPS:
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

            case RECOVER:
                statements.add(exec("sudo apt-get clean;"));
                statements.add(exec("sudo apt-get update;"));
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
        statements.add(exec("sudo apt-get clean;"));
        statements.add(exec("sudo apt-get update;"));
        statements.add(createOrOverwriteFile("/etc/chef/chef.json", ImmutableSet.of(json.toString())));
    }

    private void createNodeInstall(ImmutableList.Builder<Statement> statements) {
        List<String> installList = createInstallList();
        //Start json
        StringBuilder json = generateInstallJSON(installList);
        statements.add(exec("sudo apt-get clean;"));
        statements.add(exec("sudo apt-get update;"));
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
     * JSON generators for install list lists
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

    /*
     * JSON generator for configuration phase with recipes run list
     */
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
        //add the ip in the json
        json.append("\"ip\":\"").append(ip).append("\",");
        json.append("\"mysql_ip\":\"").append(mysql.get(0)).append("\",");
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
        //Depending on the services we generate the runlist
        Set<String> roleSet = new HashSet<String>(roles);
        //filter and keep what we want to look for
        roleSet.retainAll(filterClients);
        Iterator<String> iterRoles = roleSet.iterator();
        while (iterRoles.hasNext()) {

            String role = iterRoles.next();

            if (role.equals("ndb::mysqld")) {
                json.append("\"mysqld\"");

            }

            if (role.equals("hop::namenode")) {
                json.append("\"nn\"");

            }
            if (iterRoles.hasNext()) {
                json.append(",");
            }
        }
        json.append("]},");
        //Where the hell is this??
        json.append("\"hopagent\":{\"server\":\"").append(privateIP).append(":8080\"},");
        //need to add support for agent_user and agent_pass

        json.append("\"mysql\":{\"bind_address\":\"").append(mysql.get(0)).append("\",");
        json.append("\"root_network_acl\":\"").append(mysql.get(0)).append("\",");
        json.append("\"user\":\"kthfs\",\"user_password\":\"kthfs\",\"server_debian_password\":\"kthfs\",");
        json.append("\"server_repl_password\":\"kthfs\",\"server_root_password\":\"kthfs\"");

        json.append("},");

        /*Generate hops fragment
         /*server ip of the dashboard
         */
        json.append("\"hop\":{\"server\":\"").append(privateIP).append(":8080").append("\",");
        //rest url

        //TODO ADD SUPPORT FOR MULTIPLE nodes

        json.append("\"ndb_connectstring\":\"").append(mgms.get(0)).append("\",");
        //namenodes ips

        json.append("\"cluster\":\"").append(clusterName).append("\",");
        json.append("\"hostid\":\"").append(nodeId).append("\",");

        roleSet = new HashSet<String>(roles);

        if (roleSet.contains("hop::datanode") || roleSet.contains("hop::namenode")) {
            json.append("\"service\":\"").append("HDFS").append("\",");
        }

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
       ArrayList<String> recipes = new ArrayList<String>();
       recipes.add("ndb::install");
       recipes.add("java");
        // builder.addRecipe("hops::install");
        return new RunList.Builder().recipes(recipes).build().getRunlist();
    }

    private List<String> createRunList() {
        ArrayList<String> recipes = new ArrayList<String>();
        recipes.add("apt");
        recipes.add("python::package");
        recipes.add("java");
        recipes.add("hopagent");
        for (String role : roles) {
            recipes.add(role);
        }
        boolean collectdAdded =
                roles.contains("ndb::dn") || roles.contains("ndb::mysqld") || roles.contains("ndb:mgm")
                || roles.contains("hop::namenode") || roles.contains("hop::datanode")
                || roles.contains("hop::resourcemanager");
        //Look at the roles, if it matches add the recipes for that role

        // We always need to restart the kthfsagent after we have
        // updated its list of services
        if (collectdAdded) {
            //builder.addRecipe("kthfsagent");
            recipes.add("collect::attr-driven");
        }
        //builder.addRecipe("java::openjdk");
        recipes.add("hopagent::restart");
        return new RunList.Builder().recipes(recipes).build().getRunlist();


    }
}
