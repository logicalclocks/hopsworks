/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.kthfsdashboard.terminal;

/**
 *
 * @author jdowling
 */
public class ActiveCluster {
    private static String cluster = "";
    
    public synchronized static void setCluster(String cluster) {
        ActiveCluster.cluster = cluster;
    }

    public synchronized static String getCluster() {
        return ActiveCluster.cluster;
    }
    
}
