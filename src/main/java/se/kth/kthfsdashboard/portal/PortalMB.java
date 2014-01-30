package se.kth.kthfsdashboard.portal;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

/**
 * Portal managed bean which collects the users input data for deploying the
 * dashboard on a baremetal or cloud machine.
 * 
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class PortalMB implements Serializable {

    private static final long serialVersionUID = 7526472295622776147L;
    private String providerName;
    private String groupName;
    private String publicKey;
    private String host;
    private String privateKey;
    private String dashboardUsername;
    private String dashboardPassword;
    private String id;
    private String key;
    private String keystone;
    private String hardwareId;
    private String imageId;
    private String locationId;
    private String loginUser;
    private String nodeId;
    private boolean enableLoginUser=false;
    private boolean openstack=false;
    private boolean showProvider = false;
    private boolean publicKeyEnabled=false;

    public PortalMB() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String updateName() {
        showProvider = true;
        if (providerName.equals("OpenStack")) {
            openstack = true;
        } else {
            openstack = false;
        }
        setDefaultValues();
        return "mainPage";
    }

    public boolean isEnableLoginUser() {
        return enableLoginUser;
    }

    public void setEnableLoginUser(boolean enableLoginUser) {
        this.enableLoginUser = enableLoginUser;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getLocationId() {
        return locationId;
    }

    public void setLocationId(String locationId) {
        this.locationId = locationId;
    }

    public String getLoginUser() {
        return loginUser;
    }

    public void setLoginUser(String loginUser) {
        this.loginUser = loginUser;
    }

    public boolean isOpenstack() {
        return openstack;
    }

    public void setOpenstack(boolean openstack) {
        this.openstack = openstack;
    }

    public boolean isBaremetal() {
        return providerName.equals("Baremetal");
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKeystone() {
        return keystone;
    }

    public void setKeystone(String keystone) {
        this.keystone = keystone;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public boolean getShowProvider() {
        return showProvider;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getDashboardUsername() {
        return dashboardUsername;
    }

    public void setDashboardUsername(String dashboardUsername) {
        this.dashboardUsername = dashboardUsername;
    }

    public String getDashboardPassword() {
        return dashboardPassword;
    }

    public void setDashboardPassword(String dashboardPassword) {
        this.dashboardPassword = dashboardPassword;
    }

    public boolean isPublicKeyEnabled() {
        return publicKeyEnabled;
    }

    public void setPublicKeyEnabled(boolean publicKeyEnabled) {
        this.publicKeyEnabled = publicKeyEnabled;
    }

    public void handleLoginChange() {
    }

    private void setDefaultValues() {
        groupName = "hops";
        id = "";
        dashboardUsername = "admin";
        dashboardPassword = "admin";
        host = "";
        key = "";
        host = "";
        loginUser = "";
        publicKey = "";
        privateKey = "";
        enableLoginUser = false;
        publicKeyEnabled = false;
        if (providerName.equals("OpenStack")) {
            keystone = "";
            locationId = "";
            hardwareId = "";
            imageId = "";
        }
        else{
            locationId = "eu-west-1";
            hardwareId = "m1.large";
            imageId = "eu-west-1/ami-ffcdce8b";
        }
    }
}
