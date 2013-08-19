/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.jclouds.aws.domain.Region.DEFAULT_REGIONS;
import org.jclouds.ec2.domain.InstanceType;
import se.kth.kthfsdashboard.virtualization.clusterparser.BaremetalGroup;
import se.kth.kthfsdashboard.virtualization.clusterparser.ChefAttributes;
import se.kth.kthfsdashboard.virtualization.clusterparser.NodeGroup;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class ClusterOptions {

    private final Map<String, String> environment = new HashMap<String, String>();
    private final Map<String, String> provider = new HashMap<String, String>();
    private final Map<String, String> roles = new HashMap<String, String>();
    private final Map<String, String> services = new HashMap<String, String>();
    private final List<String> ec2Regions = new ArrayList<String>(DEFAULT_REGIONS);
    private final List<String> ec2InstanceTypes = new ArrayList<String>();
    private final Map<String, List<String>> ec2availabilityZones = new HashMap<String, List<String>>();
    
    private int portNumber;
    private String hosts="";
    private BaremetalGroup addBaremetalGroupName;
    private NodeGroup addGroupName;
    private ChefAttributes addRole;

    public ClusterOptions() {

        environment.put("Production", "prod");
        environment.put("Development", "dev");
        provider.put("Amazon", "aws-ec2");
        provider.put("OpenStack", "openstack-nova");

        roles.put("MySQLDaemon", "MySQLCluster-mysqld");
        roles.put("Management Server", "MySQLCluster-mgm");
        roles.put("NDB", "MySQLCluster-ndb");
        roles.put("Namenode", "KTHFS-namenode");
        roles.put("Datanode", "KTHFS-datanode");

        services.put("SSH", "ssh");
        services.put("Chef-Client", "chefClient");
        services.put("Chef-Server", "chefServer");
        services.put("HTTP-HTTPS", "http&https");
        services.put("WebServer", "webServer");

        ec2InstanceTypes.add(InstanceType.T1_MICRO);
        ec2InstanceTypes.add(InstanceType.M1_SMALL);
        ec2InstanceTypes.add(InstanceType.M1_MEDIUM);
        ec2InstanceTypes.add(InstanceType.M1_LARGE);
        ec2InstanceTypes.add(InstanceType.M1_XLARGE);
        ec2InstanceTypes.add(InstanceType.M2_2XLARGE);
        ec2InstanceTypes.add(InstanceType.M2_4XLARGE);
        ec2InstanceTypes.add(InstanceType.M2_XLARGE);
        ec2InstanceTypes.add(InstanceType.M3_2XLARGE);
        ec2InstanceTypes.add(InstanceType.M3_XLARGE);
        ec2InstanceTypes.add(InstanceType.C1_MEDIUM);
        ec2InstanceTypes.add(InstanceType.C1_XLARGE);
        ec2InstanceTypes.add(InstanceType.CC1_4XLARGE);
        ec2InstanceTypes.add(InstanceType.CC2_8XLARGE);
        ec2InstanceTypes.add(InstanceType.CG1_4XLARGE);
        ec2InstanceTypes.add(InstanceType.HI1_4XLARGE);
        ec2InstanceTypes.add(InstanceType.HS1_8XLARGE);

        for (String region : DEFAULT_REGIONS) {
            List<String> temp = new ArrayList<String>();
            StringBuilder zone = new StringBuilder(region);
            temp.add(zone.append('a').toString());
            zone.setCharAt(zone.length() - 1, 'b');
            temp.add(zone.toString());
            zone.setCharAt(zone.length() - 1, 'c');
            temp.add(zone.toString());
            if (!region.equals("eu-west-1")) {
                zone.setCharAt(zone.length() - 1, 'd');
                temp.add(zone.toString());
            }
            ec2availabilityZones.put(region, temp);
        }
        
        addGroupName=new NodeGroup();
        addBaremetalGroupName=new BaremetalGroup();
        addRole=new ChefAttributes();
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

    public List<String> getEc2InstanceTypes() {
        return ec2InstanceTypes;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }
   
    public Map<String, List<String>> getEc2availabilityZones() {
        return ec2availabilityZones;
    }
    
    public ChefAttributes getAddRole() {
        return addRole;
    }

    public void setAddRole(ChefAttributes addRole) {
        this.addRole = addRole;
    }

    public NodeGroup getAddGroupName() {
        return addGroupName;
    }

    public void setAddGroupName(NodeGroup addGroupName) {
        this.addGroupName = addGroupName;
    }

    public BaremetalGroup getAddBaremetalGroupName() {
        return addBaremetalGroupName;
    }

    public void setAddBaremetalGroupName(BaremetalGroup addBaremetalGroupName) {
        this.addBaremetalGroupName = addBaremetalGroupName;
    }
    
    public String getInputHosts(){
        return hosts;
    }
    public void setInputHosts(String hosts){
        this.hosts=hosts;
//       this.addBaremetalGroupName.setHosts(Arrays.asList(splittedHosts));
    }
//    public String getInputHosts(){
//        return hosts;
//    }
    
}
