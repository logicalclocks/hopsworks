/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import com.google.common.base.Function;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import org.jclouds.compute.domain.NodeMetadata;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class StoreResults implements Function<Set<? extends NodeMetadata>, Void> {

    private CountDownLatch latch;
    private List<String> rolesList;
    private Provision provision;

    public StoreResults(List<String> roles, CountDownLatch latch, Provision provision) {
        this.rolesList = roles;
        this.latch = latch;
        this.provision = provision;
    }

    @Override
    public Void apply(Set<? extends NodeMetadata> input) {

        Set<String> roles = new HashSet(rolesList);
        if (roles.contains("mgm")) {
            Iterator<? extends NodeMetadata> iter = input.iterator();
            while (iter.hasNext()) {
                //Add private ip to mgm
                NodeMetadata node = iter.next();
                provision.getMgmIP().addAll(node.getPrivateAddresses());
                provision.getMgms().put(node, rolesList);
            }
        } else if (roles.contains("ndb")) {
            Iterator<? extends NodeMetadata> iter = input.iterator();
            while (iter.hasNext()) {
                NodeMetadata node = iter.next();
                provision.getNdbsIP().addAll(node.getPrivateAddresses());
                provision.getNdbs().put(node, rolesList);
            }
        } else if (roles.contains("mysqld")) {
            Iterator<? extends NodeMetadata> iter = input.iterator();
            while (iter.hasNext()) {
                NodeMetadata node = iter.next();
                provision.getMySQLClientIP().addAll(node.getPrivateAddresses());
                provision.getMysqlds().put(node, rolesList);
            }
        } else if (roles.contains("namenode")) {
            Iterator<? extends NodeMetadata> iter = input.iterator();
            while (iter.hasNext()) {
                NodeMetadata node = iter.next();
                provision.getNamenodesIP().addAll(node.getPrivateAddresses());
                provision.getNamenodes().put(node, rolesList);
            }
        } else if (roles.contains("datanode")) {
            Iterator<? extends NodeMetadata> iter = input.iterator();
            while (iter.hasNext()) {
                NodeMetadata node = iter.next();
                provision.getDatanodes().put(node, rolesList);
            }
        }
        latch.countDown();
        return null;
    }
}
