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
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.UploadedFile;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;
import se.kth.kthfsdashboard.provision.ClusterOptions;
import se.kth.kthfsdashboard.provision.EditorYamlController;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class ClusterManagementController implements Serializable {

    @EJB
    private ClusterFacade clusterEJB;
    @ManagedProperty(value = "#{editorYamlController}")
    private EditorYamlController editorYamlController;
    private Cluster cluster;
    private Baremetal baremetalCluster;
    private UploadedFile file;
    private Yaml yaml;
    private boolean renderEC2;
    private ClusterOptions options = new ClusterOptions();
    private String clusterType = "";
    private String editType = "";
    /**
     * Variables for defining options of the cluster during the editing process
     * in the tables
     */
    private List<String> zones;
    private NodeGroup[] selectedGroup;
    private BaremetalGroup[] selectedBaremetalGroup;
    private List<NodeGroup> groups;
    private List<Integer> ports;
    private Integer[] selectedPorts;
    private List<String> globalRecipes;
    private String[] selectedGlobalRecipes;
    private NodeGroupDataModel groupsModel;
    private BaremetalGroupDataModel baremetalGroupsModel;
    private List<BaremetalGroup> baremetalGroups;
    private ClusterEntity[] selectedClusters;
    private ClusterEntity edit;
    private ClusterRepresenter repr;

    /**
     * Creates a new instance of ClusterManagementController
     */
    public ClusterManagementController() {
        repr = new ClusterRepresenter();
        repr.setPropertyUtils(new UnsortedPropertyUtils());

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        baremetalGroups = new ArrayList<BaremetalGroup>();
        baremetalGroupsModel = new BaremetalGroupDataModel(baremetalGroups);

        groups = new ArrayList<NodeGroup>();
        groupsModel = new NodeGroupDataModel(groups);
        cluster = new Cluster();
        globalRecipes = new ArrayList<String>();
        ports = new ArrayList<Integer>();
        yaml = new Yaml(repr, dumperOptions);
        yaml.setBeanAccess(BeanAccess.FIELD);

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

    public String getEditType() {
        return editType;
    }

    public void setEditType(String editType) {
        this.editType = editType;
    }

    public String getClusterType() {
        return clusterType;
    }

    public void setClusterType(String clusterType) {
        this.clusterType = clusterType;
    }

    public List<String> getZones() {
        return zones;
    }

    public void setZones(List<String> zones) {
        this.zones = zones;
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

    public String[] getSelectedGlobalRecipes() {
        return selectedGlobalRecipes;
    }

    public void setSelectedGlobalRecipes(String[] selectedGlobalRecipes) {
        this.selectedGlobalRecipes = selectedGlobalRecipes;
    }

    public List<String> getGlobalRecipes() {
        return globalRecipes;
    }

    public void setGlobalRecipes(List<String> globalRecipes) {
        this.globalRecipes = globalRecipes;
    }

    public void addPort() {
        ports.add(options.getPortNumber());
        Collections.sort(ports);
    }

    public void removePorts() {
        Collection<Integer> selection = Arrays.asList(selectedPorts);
        ports.removeAll(selection);
    }

    public void addRecipe() {
        globalRecipes.add(options.getGlobalRecipe());
        Collections.sort(globalRecipes);
    }

    public void removeGlobalRecipes() {
        Collection<String> selection = Arrays.asList(selectedGlobalRecipes);
        globalRecipes.removeAll(selection);
    }

    public NodeGroup[] getSelectedGroup() {
        return selectedGroup;
    }

    public void setSelectedGroup(NodeGroup[] selectedGroup) {
        this.selectedGroup = selectedGroup;
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

    public EditorYamlController getEditorYamlController() {
        return editorYamlController;
    }

    public void setEditorYamlController(EditorYamlController editorYamlController) {
        this.editorYamlController = editorYamlController;
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

    public void handleFileUpload(FileUploadEvent event) {
        file = event.getFile();
        String response = parseYMLtoCluster();
        FacesMessage msg = new FacesMessage(response);
        FacesContext.getCurrentInstance().addMessage(null, msg);

    }

    public void handleFileUploadOld() {
        if (file != null) {
            parseYMLtoCluster();
            FacesMessage msg = new FacesMessage("Successful", file.getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage(null, msg);
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

    private String parseYMLtoCluster() {

        try {
            Object document = yaml.load(file.getInputstream());

            if (document != null) {
                ClusterEntity entity = new ClusterEntity();

                if (document instanceof Cluster) {

                    cluster = (Cluster) document;
                    //we update the representer with the provider name.
                    //This way, when we dump the cluster back as a string in the db, it will have the missing
                    //parameters of a simple cluster
                    repr.setProvider(cluster.getProvider().getName());
                    String yamlContent = yaml.dump(cluster);
                    clusterType = "virtualized";
                    entity.setClusterType("virtualized");
                    entity.setClusterName(cluster.getName());
                    entity.setYamlContent(yamlContent);
                    clusterEJB.persistCluster(entity);
                    //we refresh again with the missing parameters the cluster
                    cluster = (Cluster) yaml.load(yamlContent);
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
            } else {
                return "Failure: document was empty or an invalid YAML file";
            }
        } catch (IOException e) {
            return "Failure: " + e.toString();
        }
        return "Successful" + file.getFileName() + " is uploaded.";
    }

    public List<ClusterEntity> listAvailableClusters() {
        //We purge any possible selections from the user when we load the clusters
        //file = null;
        cluster = null;
        baremetalCluster = null;
        return clusterEJB.findAll();
    }

    public void removeClusters() {
        if (selectedClusters != null && selectedClusters.length > 0) {

            Collection<ClusterEntity> selections = Arrays.asList(selectedClusters);
            for (ClusterEntity selection : selections) {
                clusterEJB.removeCluster(selection);
            }
        }
    }

    public String proceedCreateWizard() {
        edit = null;
        globalRecipes.clear();
        ports.clear();
        baremetalGroups.clear();
        groups.clear();
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
            String response = parseYMLtoCluster();
            if (response.startsWith("Successful")) {
                return "proceedLaunchCluster";
            } else {
                FacesMessage msg = new FacesMessage(response);
                FacesContext.getCurrentInstance().addMessage(null, msg);
                return "";
            }
        } else {
            FacesMessage msg = new FacesMessage("Error. You must first select a file, then click the upload button.");
            FacesContext.getCurrentInstance().addMessage(null, msg);

            return "";
        }
    }

    public String onFlowProcess(FlowEvent event) {

        if (event.getNewStep().toLowerCase().indexOf("provider") >= 0) {

            persistPorts();
            persistGlobalRecipes();
        }
        if (event.getNewStep().toLowerCase().indexOf("confirm") >= 0) {
            persistNodes();
        }

        return event.getNewStep();

    }

    public void generateYAML(ActionEvent event) {

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

//    public String editSelection() {
//        if (selectedClusters.length != 0) {
//            edit = selectedClusters[0];
//            clusterType = edit.getClusterType();
//            Object document = yaml.load(edit.getYamlContent());
//            if (document != null && document instanceof Cluster) {
//
//                cluster = (Cluster) document;
//                ports = new ArrayList<Integer>(cluster.getGlobal().getAuthorizePorts());
//                groups = new ArrayList<NodeGroup>(cluster.getNodes());
//                groupsModel = new NodeGroupDataModel(groups);
//
//                zones = options.getEc2availabilityZones().get(cluster.getProvider().getRegion());
//                if (cluster.getProvider().getName().equals("aws-ec2")) {
//                    renderEC2 = true;
//                }
//            } else if (document != null && document instanceof Baremetal) {
//                //probably need to check references like in the cluster case
//                baremetalCluster = (Baremetal) document;
//                baremetalGroups = new ArrayList<BaremetalGroup>(baremetalCluster.getNodes());
//                baremetalGroupsModel = new BaremetalGroupDataModel(baremetalGroups);
//
//            } else {
//                cluster = null;
//            }
//        }
//
//        return "createClusterWizard";
//    }
    public String deleteSelection() {
        if (selectedClusters != null && selectedClusters.length != 0) {
            Collection<ClusterEntity> selections = Arrays.asList(selectedClusters);
            for (ClusterEntity selection : selections) {
                clusterEJB.removeCluster(selection);
            }
            return "TO DO";
        } else {
            return "No cluster selected";
        }
    }

    public String editSelection() {
        if (selectedClusters != null && selectedClusters.length != 0 && editType.equals("editor")) {
            edit = selectedClusters[0];
            editorYamlController.setEntity(edit);
            editorYamlController.init();
            return "editClusterEditor";
        } else if (selectedClusters != null && selectedClusters.length != 0 && editType.equals("wizard")) {
            edit = selectedClusters[0];
            clusterType = edit.getClusterType();
            Object document = yaml.load(edit.getYamlContent());
            if (document != null && document instanceof Cluster) {

                cluster = (Cluster) document;
                ports = new ArrayList<Integer>(cluster.getGlobal().getAuthorizePorts());
                groups = new ArrayList<NodeGroup>(cluster.getNodes());
                groupsModel = new NodeGroupDataModel(groups);

                zones = options.getEc2availabilityZones().get(cluster.getProvider().getRegion());
                if (cluster.getProvider().getName().equals("aws-ec2")) {
                    renderEC2 = true;
                }
            } else if (document != null && document instanceof Baremetal) {
                //probably need to check references like in the cluster case
                baremetalCluster = (Baremetal) document;
                baremetalGroups = new ArrayList<BaremetalGroup>(baremetalCluster.getNodes());
                baremetalGroupsModel = new BaremetalGroupDataModel(baremetalGroups);

            } else {
                cluster = null;
            }
            return "createClusterWizard";
        } else {
            System.out.println("hello");
            return "";
        }
    }

    public String loadSelection() {
        if (selectedClusters != null && selectedClusters.length != 0) {
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
        if (selectedClusters != null && selectedClusters.length != 0) {
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

    /*
     * Form Modifiers to generate the cluster
     */
    public void addGroup() {
        if (isBaremetal()) {

            options.getAddBaremetalGroup().setStringRecipes(options.getInputRecipes());
            options.getAddBaremetalGroup().setStringHosts(options.getInputHosts());
            baremetalGroups.add(options.getAddBaremetalGroup());
            options.setAddBaremetalGroup(new BaremetalGroup());
            options.setInputHosts("");
            options.setInputRecipes("");

        } else {

            options.getAddGroup().setStringPorts(options.getInputPorts());
            options.getAddGroup().setStringRecipes(options.getInputRecipes());
            groups.add(options.getAddGroup());
            options.setAddGroup(new NodeGroup());
            options.setInputPorts("");
            options.setInputRecipes("");

        }
    }

    public void editSelectedGroup() {

        if (!isBaremetal() && selectedGroup.length > 0) {

            options.setEditGroup(selectedGroup[0]);
            options.setEditPorts(selectedGroup[0].getStringPorts());
            options.setEditRecipes(selectedGroup[0].getStringRecipes());
//            RequestContext context =RequestContext.getCurrentInstance();
//            context.update("mainForm:editDig");
//            context.execute("editGroup.show()");
//            System.out.println("hello");

        } else if (isBaremetal() && selectedBaremetalGroup.length > 0) {
            options.setEditBaremetalGroup(selectedBaremetalGroup[0]);
            options.setEditHosts(selectedBaremetalGroup[0].getStringHosts());
            options.setEditRecipes(selectedBaremetalGroup[0].getStringRecipes());
//            RequestContext context =RequestContext.getCurrentInstance();
//            context.update("mainForm:editDig");
//            context.execute("editGroup.show()");
//            System.out.println("bye");
        }
    }

    public void editGroup() {
        if (isBaremetal()) {
            options.getEditBaremetalGroup().setStringHosts(options.getEditHosts());
            options.getEditBaremetalGroup().setStringRecipes(options.getEditRecipes());
            options.setEditBaremetalGroup(new BaremetalGroup());
            options.setInputHosts("");
            options.setInputRecipes("");
        } else {
            options.getEditGroup().setStringPorts(options.getEditPorts());
            options.getEditGroup().setStringRecipes(options.getEditRecipes());
            options.setEditGroup(new NodeGroup());
            options.setEditPorts("");
            options.setEditRecipes("");
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
            cluster.getGlobal().getAuthorizePorts().clear();
            cluster.getGlobal().getAuthorizePorts().addAll(ports);
        }
    }

    private void persistGlobalRecipes() {
        if (isBaremetal()) {
            baremetalCluster.getGlobal().getRecipes().clear();
            baremetalCluster.getGlobal().getRecipes().addAll(globalRecipes);
        } else {
            cluster.getGlobal().getRecipes().clear();
            cluster.getGlobal().getRecipes().addAll(globalRecipes);
        }
    }
}
