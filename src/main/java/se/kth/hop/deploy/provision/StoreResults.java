/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.deploy.provision;

import com.google.common.base.Function;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
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
        if (input != null) {
            Set<String> roles = new HashSet(rolesList);
//            if (roles.contains("ndb::mgm")) {
//                Iterator<? extends NodeMetadata> iter = input.iterator();
//                while (iter.hasNext()) {
//                    //Add private ip to mgm
//                    NodeMetadata node = iter.next();
//                    provision.getMgmIP().addAll(node.getPrivateAddresses());
//                    List<String> mgmRecipes = new LinkedList();
//                    mgmRecipes.addAll(rolesList);
//                    mgmRecipes.remove("ndb::mysqld");
//                    mgmRecipes.remove("ndb::dn");
//                    provision.getMgms().put(node, mgmRecipes);
//                }
//            } else if (roles.contains("ndb::dn")) {
//                Iterator<? extends NodeMetadata> iter = input.iterator();
//                while (iter.hasNext()) {
//                    NodeMetadata node = iter.next();
//                    provision.getNdbsIP().addAll(node.getPrivateAddresses());
//                    List<String> ndbRecipes = new LinkedList();
//                    ndbRecipes.addAll(rolesList);
//                    ndbRecipes.remove("ndb::mysqld");
//                    ndbRecipes.remove("ndb::mgm");
//                    provision.getNdbs().put(node, ndbRecipes);
//                }
//            } else if (roles.contains("ndb::mysqld")) {
//                Iterator<? extends NodeMetadata> iter = input.iterator();
//                while (iter.hasNext()) {
//                    NodeMetadata node = iter.next();
//                    provision.getMySQLClientIP().addAll(node.getPrivateAddresses());
//                    List<String> mysqldRecipes = new LinkedList();
//                    mysqldRecipes.addAll(rolesList);
//                    mysqldRecipes.remove("ndb::mgm");
//                    mysqldRecipes.remove("ndb::dn");
//                    provision.getMysqlds().put(node, mysqldRecipes);
//                }
//            } else if (roles.contains("hop::namenode")) {
//                Iterator<? extends NodeMetadata> iter = input.iterator();
//                while (iter.hasNext()) {
//                    NodeMetadata node = iter.next();
//                    provision.getNamenodesIP().addAll(node.getPrivateAddresses());
//                    provision.getNamenodes().put(node, rolesList);
//                }
//            } else if (roles.contains("hop::datanode")) {
//                Iterator<? extends NodeMetadata> iter = input.iterator();
//                while (iter.hasNext()) {
//                    NodeMetadata node = iter.next();
//                    provision.getDatanodes().put(node, rolesList);
//                }
//            }

            Iterator<? extends NodeMetadata> iter = input.iterator();
            while (iter.hasNext()) {
                NodeMetadata node = iter.next();
                if (roles.contains("ndb::mgm")) {
                    provision.getMgmIP().addAll(node.getPrivateAddresses());
                    List<String> mgmRecipes = new LinkedList();
                    mgmRecipes.addAll(rolesList);
                    mgmRecipes.remove("ndb::mysqld");
                    mgmRecipes.remove("ndb::dn");
                    provision.getMgms().put(node, mgmRecipes);
                } else if (roles.contains("ndb::dn")) {
                    provision.getNdbsIP().addAll(node.getPrivateAddresses());
                    List<String> ndbRecipes = new LinkedList();
                    ndbRecipes.addAll(rolesList);
                    ndbRecipes.remove("ndb::mysqld");
                    ndbRecipes.remove("ndb::mgm");
                    provision.getNdbs().put(node, ndbRecipes);
                } else if (roles.contains("ndb::mysqld")) {
                    provision.getMySQLClientIP().addAll(node.getPrivateAddresses());
                    List<String> mysqldRecipes = new LinkedList();
                    mysqldRecipes.addAll(rolesList);
                    mysqldRecipes.remove("ndb::mgm");
                    mysqldRecipes.remove("ndb::dn");
                    provision.getMysqlds().put(node, mysqldRecipes);
                } else if (roles.contains("hop::namenode")) {
                    provision.getNamenodesIP().addAll(node.getPrivateAddresses());
                    provision.getNamenodes().put(node, rolesList);
                } else if (roles.contains("hop::datanode")) {
                    provision.getDatanodes().put(node, rolesList);
                }
            }
        }
        latch.countDown();
        return null;
    }
}
