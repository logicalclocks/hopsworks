
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
    private boolean installPhase;
    private GlobalProperties global = new GlobalProperties();
    private Provider provider = new Provider();
    private List<NodeGroup> nodes = new ArrayList<NodeGroup>();


    public boolean isInstallPhase() {
        return installPhase;
    }

    public void setInstallPhase(boolean installPhase) {
        this.installPhase = installPhase;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
   
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
