/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization;

import com.sun.xml.internal.ws.api.ha.StickyFeature;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.jclouds.aws.domain.Region.DEFAULT_REGIONS;


/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class ClusterOptions {
    
    private final Map<String,String> environment= new HashMap<String, String>();
    private final Map<String,String> provider= new HashMap<String, String>();
    private final Map<String,String> roles= new HashMap<String, String>();
    private final Map<String,String> services= new HashMap<String, String>();
    private final List<String> ec2Regions= new ArrayList<String>(DEFAULT_REGIONS);
    
    public  ClusterOptions(){
        
        environment.put("Production","prod");
        environment.put("Development","dev");
        provider.put("Amazon","aws-ec2");
        provider.put("OpenStack","openstack-nova");
        
        roles.put("MySQLDaemon","MySQLCluster-mysqld");
        roles.put("Management Server", "MySQLCluster-mgm");
        roles.put("NDB","MySQLCluster-ndb");
        roles.put("Namenode","KTHFS-namenode");
        roles.put("Datanode","KTHFS-datanode");
        
        services.put("SSH", "ssh");
        services.put("Chef-Client","chefClient");
        services.put("Chef-Server","chefServer");
        services.put("HTTP-HTTPS","http&https");
        services.put("WebServer","webServer");
        
        
    }

    public Map<String, String> getProvider() {
        return provider;
    }

    public Map<String, String> getRoles() {
        return roles;
    }

    public Map<String, String> getServices() {
        return services;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public List<String> getEc2Regions() {
        return ec2Regions;
    }
    
    
    
    
    
}
