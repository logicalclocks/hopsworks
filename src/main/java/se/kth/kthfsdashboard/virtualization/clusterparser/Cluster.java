
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */

public class Cluster implements Serializable{

    private String name;
//    private String environment;
    private boolean installPhase;
    private GlobalProperties global = new GlobalProperties();
//    private List<String> globalServices=new ArrayList<String>();
//    private List<String> authorizePorts=new ArrayList<String>();
//    private List<Integer> authorizeSpecificPorts=new ArrayList<Integer>();
    private Provider provider = new Provider();
    private List<NodeGroup> nodes = new ArrayList<NodeGroup>();

//    private List<ChefAttributes> chefAttributes= new ArrayList<ChefAttributes>();

//    public List<String> getGlobalServices() {
//        return globalServices;
//    }
//
//    public void setGlobalServices(List<String> globalServices) {
//        this.globalServices = globalServices;
//    }
//
//    public List<String> getAuthorizePorts() {
//        return authorizePorts;
//    }

    public boolean isInstallPhase() {
        return installPhase;
    }

    public void setInstallPhase(boolean installPhase) {
        this.installPhase = installPhase;
    }

//    public void setAuthorizePorts(List<String> authorizePorts) {
//        this.authorizePorts = authorizePorts;
//    }
//
//    public List<Integer> getAuthorizeSpecificPorts() {
//        return authorizeSpecificPorts;
//    }
//
//    public void setAuthorizeSpecificPorts(List<Integer> authorizeSpecificPorts) {
//        this.authorizeSpecificPorts = authorizeSpecificPorts;
//    }
   
//    public List<ChefAttributes> getChefAttributes() {
//        return chefAttributes;
//    }
//
//    public void setChefAttributes(List<ChefAttributes> chefAttributes) {
//        this.chefAttributes = chefAttributes;
//    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

//    public String getEnvironment() {
//        return environment;
//    }
//
//    public void setEnvironment(String environment) {
//        this.environment = environment;
//    }
    
    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }
   
    public List<NodeGroup> getNodes() {
        return nodes;
    }

    public void setNodes(List<NodeGroup> nodes) {
        this.nodes = nodes;
    }

    public GlobalProperties getGlobal() {
        return global;
    }

    public void setGlobal(GlobalProperties global) {
        this.global = global;
    }
    
    @Override
    public String toString() {
        return "Cluster{" + "name=" + name + ", provider=" + provider + ", instances=" + nodes + '}';
    }
        
}
