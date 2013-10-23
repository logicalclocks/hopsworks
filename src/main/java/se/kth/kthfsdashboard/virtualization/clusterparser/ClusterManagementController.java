/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import se.kth.kthfsdashboard.provision.ClusterOptions;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class ClusterManagementController implements Serializable {

    @EJB
    private ClusterFacade clusterEJB;
    private Cluster cluster;
    private Baremetal baremetalCluster;
    private UploadedFile file;
    private Yaml yaml = new Yaml();
    private boolean skipChef;
    private boolean renderEC2;
    private ClusterOptions options = new ClusterOptions();
    private static Logger logger = Logger.getLogger(Cluster.class.getName());
    private String clusterType;
    /**
     * Variables for defining options of the cluster during the editing process
     * in the tables
     */
    private List<String> zones;
    private NodeGroup[] selectedGroup;
    private BaremetalGroup[] selectedBaremetalGroup;
    private ChefAttributes[] selectedAttributes;
    private List<NodeGroup> groups;
    private List<Integer> ports;
    private Integer[] selectedPorts;
    private NodeGroupDataModel groupsModel;
    private BaremetalGroupDataModel baremetalGroupsModel;
    private List<BaremetalGroup> baremetalGroups;
    private ChefAttributeDataModel chefAttributesModel;
    private List<ChefAttributes> chefAttributes;
    private ClusterEntity[] selectedClusters;
    private ClusterEntity edit;

    /**
     * Creates a new instance of ClusterManagementController
     */
    public ClusterManagementController() {

        baremetalGroups = new ArrayList<BaremetalGroup>();
        baremetalGroupsModel = new BaremetalGroupDataModel(baremetalGroups);

        groups = new ArrayList<NodeGroup>();
        groupsModel = new NodeGroupDataModel(groups);

        ports = new ArrayList<Integer>();
        chefAttributes = new ArrayList<ChefAttributes>();
        chefAttributesModel = new ChefAttributeDataModel(chefAttributes);
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

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public void handleFileUpload() {
        if (file != null) {
            parseYMLtoCluster();
            FacesMessage msg = new FacesMessage("Successful", file.getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
        }
    }

    public List<String> getZones() {
        return zones;
    }

    public void setZones(List<String> zones) {
        this.zones = zones;
    }

    public boolean isSkipChef() {
        return skipChef;
    }

    public void setSkipChef(boolean skip) {
        this.skipChef = skip;
    }

    public ClusterOptions getOptions() {
        return options;
    }

    public void setOptions(ClusterOptions options) {
        this.options = options;
    }

    public boolean isRenderEC2() {
        return renderEC2;
    }

    public void setRenderEC2(boolean renderEC2) {
        this.renderEC2 = renderEC2;
    }

    public NodeGroupDataModel getGroupsModel() {
        return groupsModel;
    }

    public BaremetalGroupDataModel getBaremetalGroupsModel() {
        return baremetalGroupsModel;
    }

    public Integer[] getSelectedPorts() {
        return selectedPorts;
    }

    public void setSelectedPorts(Integer[] selectedPorts) {
        this.selectedPorts = selectedPorts;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }

    public Baremetal getBaremetalCluster() {
        return baremetalCluster;
    }

    public void setBaremetalCluster(Baremetal baremetalCluster) {
        this.baremetalCluster = baremetalCluster;
    }

    public BaremetalGroup[] getSelectedBaremetalGroup() {
        return selectedBaremetalGroup;
    }

    public void setSelectedBaremetalGroup(BaremetalGroup[] selectedBaremetalGroup) {
        this.selectedBaremetalGroup = selectedBaremetalGroup;
    }

    public ChefAttributes[] getSelectedAttributes() {
        return selectedAttributes;
    }

    public void setSelectedAttributes(ChefAttributes[] selectedAttributes) {
        this.selectedAttributes = selectedAttributes;
    }

    public ChefAttributeDataModel getChefAttributesModel() {
        return chefAttributesModel;
    }

    public void setChefAttributesModel(ChefAttributeDataModel chefAttributesModel) {
        this.chefAttributesModel = chefAttributesModel;
    }

    public ClusterEntity[] getSelectedClusters() {
        return selectedClusters;
    }

    public void setSelectedClusters(ClusterEntity[] selectedClusters) {
        this.selectedClusters = selectedClusters;
    }

    public boolean isBaremetal() {
        return clusterType.equals("baremetal");
    }

    /*
     * Event Handlers
     */
    public void handleProviderChange() {
        if (cluster.getProvider().getName().equals("aws-ec2")) {
            renderEC2 = true;
        } else {
            renderEC2 = false;
        }

    }

    public void handleRegionChange() {

        if (cluster.getProvider().getName().equals("aws-ec2")
                && !cluster.getProvider().getRegion().equals("")) {
            zones = new ArrayList<String>();
            zones = options.getEc2availabilityZones().get(cluster.getProvider().getRegion());

        } else {
            zones = new ArrayList<String>();
        }

    }

    private void parseYMLtoCluster() {

        try {
            yaml.setBeanAccess(BeanAccess.FIELD);
            Object document = yaml.load(file.getInputstream());

            if (document != null) {
                ClusterEntity entity = new ClusterEntity();

                if (document instanceof Cluster) {

                    cluster = (Cluster) document;
                    String yamlContent = yaml.dump(cluster);
                    clusterType = "virtualized";
                    entity.setClusterType("virtualized");
                    entity.setClusterName(cluster.getName());
                    entity.setYamlContent(yamlContent);
                    clusterEJB.persistCluster(entity);
                } else if (document instanceof Baremetal) {
                    baremetalCluster = (Baremetal) document;
                    String yamlContent = yaml.dump(baremetalCluster);
                    clusterType = "baremetal";
                    entity.setClusterType("baremetal");
                    entity.setClusterName(baremetalCluster.getName());
                    entity.setYamlContent(yamlContent);
                    clusterEJB.persistCluster(entity);
                } else {
                    cluster = null;
                    baremetalCluster = null;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public List<ClusterEntity> listAvailableClusters() {
        //We purge any possible selections from the user when we load the clusters
        //file = null;
        cluster = null;
        baremetalCluster = null;
        return clusterEJB.findAll();
    }

    public void removeClusters() {
        Collection<ClusterEntity> selection = Arrays.asList(selectedClusters);
        for (ClusterEntity selections : selection) {
            clusterEJB.removeCluster(selections);
        }
    }

    public String proceedCreateWizard() {
        if (!isBaremetal()) {
            cluster = new Cluster();
            cluster.getProvider().setImage("eu-west-1/ami-ffcdce8b");
            cluster.setInstallPhase(false);
            baremetalCluster = null;
        } else {
            baremetalCluster = new Baremetal();
            cluster = null;
        }

        return "createClusterWizard";
    }

    public String proceedLauncher() {
        if (file != null) {
            return "proceedLaunchCluster";
        } else {
            return "";
        }
    }

    public String onFlowProcess(FlowEvent event) {
        logger.info("Current wizard step: " + event.getOldStep());
        logger.info("Next step: " + event.getNewStep());
        if (skipChef) {
            skipChef = false;	//reset in case user goes back
            persistNodes();
            return "confirm";
        } else {
            if (event.getNewStep().toLowerCase().indexOf("provider") >= 0) {
                persistPorts();
            }
            if (event.getNewStep().toLowerCase().indexOf("chef") >= 0) {
                persistNodes();
            }
            if (event.getNewStep().toLowerCase().indexOf("confirm") >= 0) {
                persistChef();
            }
            return event.getNewStep();
        }

    }

    /*
     * Form Modifiers to generate the cluster
     */
    public void addChefJson() {
        chefAttributes.add(options.getAddRole());
        options.setAddRole(new ChefAttributes());
    }

    public void removeChefJson() {
        Collection<ChefAttributes> selection = Arrays.asList(selectedAttributes);
        chefAttributes.removeAll(selection);
    }

    public void addGroup() {
        if (isBaremetal()) {
            BaremetalGroup temp = options.getAddBaremetalGroupName();
            String[] hosts = options.getInputHosts().split(",");

            temp.setHosts(Arrays.asList(hosts));
            baremetalGroups.add(temp);
            options.setAddBaremetalGroupName(new BaremetalGroup());
            options.setInputHosts("");
        } else {
            groups.add(options.getAddGroupName());
            options.setAddGroupName(new NodeGroup());
        }
    }

    public void removeGroup() {
        if (isBaremetal()) {
            Collection<BaremetalGroup> selection = Arrays.asList(selectedBaremetalGroup);
            baremetalGroups.removeAll(selection);
        } else {
            Collection<NodeGroup> selection = Arrays.asList(selectedGroup);
            groups.removeAll(selection);
        }
    }

    public void addPort() {
        ports.add(options.getPortNumber());
        Collections.sort(ports);
    }

    public void removePorts() {
        Collection<Integer> selection = Arrays.asList(selectedPorts);
        ports.removeAll(selection);
    }

    public NodeGroup[] getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(NodeGroup[] selectedGroup) {
        this.selectedGroup = selectedGroup;
    }

    private void persistNodes() {
        if (isBaremetal()) {
            baremetalCluster.getNodes().clear();
            baremetalCluster.getNodes().addAll(baremetalGroups);
        } else {
            cluster.getNodes().clear();
            cluster.getNodes().addAll(groups);
        }

    }

    private void persistPorts() {
        if (!isBaremetal()) {
//            cluster.getAuthorizeSpecificPorts().clear();
//            cluster.getAuthorizeSpecificPorts().addAll(ports);
        }
    }

    private void persistChef() {
        if (isBaremetal()) {
//            baremetalCluster.getChefAttributes().clear();
//            baremetalCluster.getChefAttributes().addAll(chefAttributes);
//        } else {
//            if (cluster.getChefAttributes() != null) {
//                cluster.getChefAttributes().clear();
//                cluster.getChefAttributes().addAll(chefAttributes);
//            }
        }
    }

    public void generateTable(ActionEvent event) {
        if (edit != null) {
            edit.setClusterType(clusterType);

            if (clusterType.equals("baremetal")) {
                edit.setClusterName(baremetalCluster.getName());
                edit.setYamlContent(yaml.dump(baremetalCluster));
            } else {
                edit.setClusterName(cluster.getName());
                edit.setYamlContent(yaml.dump(cluster));
            }
            clusterEJB.updateCluster(edit);
        } else {
            ClusterEntity entity = new ClusterEntity();
            entity.setClusterType(clusterType);
            if (clusterType.equals("baremetal")) {
                entity.setClusterName(baremetalCluster.getName());
                entity.setYamlContent(yaml.dump(baremetalCluster));
            } else {
                entity.setClusterName(cluster.getName());
                entity.setYamlContent(yaml.dump(cluster));
            }
            clusterEJB.persistCluster(entity);
        }

    }

    public String editSelection() {
        if (selectedClusters.length != 0) {
            edit = selectedClusters[0];
            String content = edit.getYamlContent();
            clusterType = edit.getClusterType();
            Object document = yaml.load(edit.getYamlContent());
            if (document != null && document instanceof Cluster) {

                cluster = (Cluster) document;
                ports = new ArrayList<Integer>(cluster.getGlobal().getAuthorizePorts());

//                if (cluster.getChefAttributes() != null) {
//
//                    chefAttributes = new ArrayList<ChefAttributes>(cluster.getChefAttributes());
//                    chefAttributesModel = new ChefAttributeDataModel(chefAttributes);
//                } else {
//                    chefAttributes = new ArrayList<ChefAttributes>();
//                    cluster.setChefAttributes(chefAttributes);
//                    chefAttributesModel = new ChefAttributeDataModel(chefAttributes);
//                }
                groups = new ArrayList<NodeGroup>(cluster.getNodes());
                groupsModel = new NodeGroupDataModel(groups);

                zones = options.getEc2availabilityZones().get(cluster.getProvider().getRegion());
                if (cluster.getProvider().getName().equals("aws-ec2")) {
                    renderEC2 = true;
                }
            } else if (document != null && document instanceof Baremetal) {
                //probably need to check references like in the cluster case
                baremetalCluster = (Baremetal) document;
//                if (baremetalCluster.getChefAttributes() != null) {
//
//                    chefAttributes = new ArrayList<ChefAttributes>(baremetalCluster.getChefAttributes());
//                    chefAttributesModel = new ChefAttributeDataModel(chefAttributes);
//                } else {
//                    chefAttributes = new ArrayList<ChefAttributes>();
//                    baremetalCluster.setChefAttributes(chefAttributes);
//                    chefAttributesModel = new ChefAttributeDataModel(chefAttributes);
//                }

                baremetalGroups = new ArrayList<BaremetalGroup>(baremetalCluster.getNodes());
                baremetalGroupsModel = new BaremetalGroupDataModel(baremetalGroups);

            } else {
                cluster = null;
            }
        }

        return "createClusterWizard";
    }

    public String loadSelection() {
        if (selectedClusters.length != 0) {
            ClusterEntity selection = selectedClusters[0];
            String content = selection.getYamlContent();
            clusterType = selection.getClusterType();
            System.out.println(content);
            Object document = yaml.load(selection.getYamlContent());
            if (document != null && document instanceof Cluster) {
                cluster = (Cluster) document;
            } else if (document != null && document instanceof Baremetal) {
                baremetalCluster = (Baremetal) document;
            } else {
                cluster = null;
            }
        }
        return "proceedLaunchCluster";
    }

    public StreamedContent exportCluster() {
        DefaultStreamedContent export = null;
        if (selectedClusters.length != 0) {
            ClusterEntity selection = selectedClusters[0];
            String content = selection.getYamlContent();

            if (selection != null) {
                try {
                    InputStream stream = new ByteArrayInputStream(content.getBytes("UTF-8"));
                    export = new DefaultStreamedContent(stream, "text/yml", selection.getClusterName() + ".yml");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        }
        return export;
    }
}
