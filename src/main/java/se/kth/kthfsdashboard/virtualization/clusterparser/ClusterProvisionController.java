/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.UploadedFile;
import org.yaml.snakeyaml.Yaml;
import se.kth.kthfsdashboard.virtualization.ClusterOptions;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class ClusterProvisionController implements Serializable {

    @EJB
    private ClusterFacade clusterEJB;
    private Cluster cluster= new Cluster();
    private UploadedFile file;
    private Yaml yaml = new Yaml();
    private int option;
    private boolean skip;
    private ClusterOptions options= new ClusterOptions();
    private TreeNode root;
    private TreeNode selectedItem;
    private TreeNode[] selectedItems;
    private static Logger logger = Logger.getLogger(Cluster.class.getName());

    /**
     * Creates a new instance of ClusterProvisionController
     */
    public ClusterProvisionController() {
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public void handleFileUpload() {
        if ((file != null) && (option == 3)) {
            parseYMLtoCluster();
            generateTreeTable();
            FacesMessage msg = new FacesMessage("Successful", file.getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public int getOption() {
        return option;
    }

    public void setOption(int option) {
        this.option = option;
    }

    public boolean isSkip() {
        return skip;
    }

    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    public ClusterOptions getOptions() {
        return options;
    }

    public void setOptions(ClusterOptions options) {
        this.options = options;
    }

    
    public TreeNode getRoot() {
        return root;
    }

    public TreeNode getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(TreeNode selectedItem) {
        this.selectedItem = selectedItem;
    }

    public TreeNode[] getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(TreeNode[] selectedItems) {
        this.selectedItems = selectedItems;
    }

    private void parseYMLtoCluster() {
        try {
            Object document = yaml.load(file.getInputstream());

            if (document != null && document instanceof Cluster) {

                cluster = (Cluster) document;
                clusterEJB.persistCluster(cluster);
            } else {

                cluster = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public String proceedOption() {
        String result;
        switch (option) {
            case 1:
                result = "createClusterWizard";
                break;
            case 2:
                result = "welcome";
                break;
            case 3:
                result = "proceedLaunchCluster";
                break;
            default:
                result = "welcome";
                break;
        }
        return result;

    }

    public String onFlowProcess(FlowEvent event) {
        logger.info("Current wizard step:" + event.getOldStep());
        logger.info("Next step:" + event.getNewStep());
        if (skip) {
            skip = false;	//reset in case user goes back
            return "confirm";
        } else {
            return event.getNewStep();
        }

    }

    public void save(ActionEvent actionEvent) {
        //Persist user  

        FacesMessage msg = new FacesMessage("Successful", "Welcome :" + cluster.getName());
        FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    private void generateTreeTable() {
        //System.out.println(clusterEJB.findAll());
        root = new DefaultTreeNode("root", null);


        int namenode = 0;
        int datanode = 0;
        int mgmd = 0;
        int ndbd = 0;
        int mysql = 0;

        for (NodeGroup group : cluster.getNodes()) {
            if (group.getRoles().contains("KTHFS-namenode")) {
                namenode += group.getNumber();
            }
            if (group.getRoles().contains("KTHFS-datanode")) {
                datanode += group.getNumber();
            }
            if (group.getRoles().contains("MySQLCluster-mgm")) {
                mgmd += group.getNumber();
            }
            if (group.getRoles().contains("MySQLCluster-mysqld")) {
                mysql += group.getNumber();
            }
            if (group.getRoles().contains("MySQLCluster-ndb")) {
                ndbd += group.getNumber();
            }

        }

        if (namenode != 0 || datanode != 0) {
            TreeNode node1 = new DefaultTreeNode("node", new InstanceElement("KTHFS", datanode + namenode, ""), root);
            if (namenode != 0) {
                TreeNode node11 = new DefaultTreeNode("leaf", new InstanceElement("namenode", namenode, "pending"), node1);
            }
            if (datanode != 0) {
                TreeNode node12 = new DefaultTreeNode("leaf", new InstanceElement("datanode", datanode, "pending"), node1);
            }
        }


        if (mgmd != 0 || ndbd != 0 || mysql != 0) {
            TreeNode node2 = new DefaultTreeNode("node", new InstanceElement("MYSQL", mgmd + ndbd + mysql, ""), root);
            if (mgmd != 0) {
                TreeNode node21 = new DefaultTreeNode("leaf", new InstanceElement("mgmd", mgmd, "pending"), node2);
            }
            if (ndbd != 0) {
                TreeNode node22 = new DefaultTreeNode("leaf", new InstanceElement("ndbd", ndbd, "pending"), node2);
            }
            if (mysql != 0) {
                TreeNode node23 = new DefaultTreeNode("leaf", new InstanceElement("mysql", mysql, "pending"), node2);
            }
        }
    }
}
