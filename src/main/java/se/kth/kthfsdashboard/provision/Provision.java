/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import java.util.List;
import java.util.Map;
import org.jclouds.compute.domain.NodeMetadata;

/**
 * Interface which defines the basic operations that specifies a provisioning context
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public interface Provision {

    public void initializeCluster();

    public boolean launchNodesBasicSetup();

    public void installPhase();

    public void deployingConfigurations();

    public List<String> getNdbsIP();

    public List<String> getMgmIP();

    public List<String> getMySQLClientIP();

    public List<String> getNamenodesIP();

    public Map<NodeMetadata, List<String>> getMgms();

    public Map<NodeMetadata, List<String>> getNdbs();

    public Map<NodeMetadata, List<String>> getMysqlds();

    public Map<NodeMetadata, List<String>> getNamenodes();

    public Map<NodeMetadata, List<String>> getDatanodes();
}
