package se.kth.hop.portal;

import java.io.Serializable;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class PortalCredentialsMB implements Serializable{

    private boolean awsec2=false;
    private String awsec2Id;
    private String awsec2Key;
    private boolean openstack=false;
    private String openstackId;
    private String openstackKey;
    private String openstackKeystone;
    private boolean rackspace=false;
    private String rackspaceId;
    private String rackspaceKey;
    private String hardwareId;
    private String imageId;
    private String locationId;
    private String loginUser;
    private boolean enableLoginUser;
      

    /**
     * Creates a new instance of PortalCredentialsMB
     */
    public PortalCredentialsMB() {
    }

    public boolean isAwsec2() {
        return awsec2;
    }

    public void setAwsec2(boolean awsec2) {
        this.awsec2 = awsec2;
    }

    public String getAwsec2Id() {
        return awsec2Id;
    }

    public void setAwsec2Id(String awsec2Id) {
        this.awsec2Id = awsec2Id;
    }

    public String getAwsec2Key() {
        return awsec2Key;
    }

    public void setAwsec2Key(String awsec2Key) {
        this.awsec2Key = awsec2Key;
    }

    public boolean isOpenstack() {
        return openstack;
    }

    public void setOpenstack(boolean openstack) {
        this.openstack = openstack;
    }

    public String getOpenstackId() {
        return openstackId;
    }

    public void setOpenstackId(String openstackId) {
        this.openstackId = openstackId;
    }

    public String getOpenstackKey() {
        return openstackKey;
    }

    public void setOpenstackKey(String openstackKey) {
        this.openstackKey = openstackKey;
    }

    public boolean isRackspace() {
        return rackspace;
    }

    public void setRackspace(boolean rackspace) {
        this.rackspace = rackspace;
    }

    public String getRackspaceId() {
        return rackspaceId;
    }

    public void setRackspaceId(String rackspaceId) {
        this.rackspaceId = rackspaceId;
    }

    public String getRackspaceKey() {
        return rackspaceKey;
    }

    public void setRackspaceKey(String rackspaceKey) {
        this.rackspaceKey = rackspaceKey;
    }

    public String getOpenstackKeystone() {
        return openstackKeystone;
    }

    public void setOpenstackKeystone(String openstackKeystone) {
        this.openstackKeystone = openstackKeystone;
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

    public boolean isEnableLoginUser() {
        return enableLoginUser;
    }

    public void setEnableLoginUser(boolean enableLoginUser) {
        this.enableLoginUser = enableLoginUser;
    }
        
    public void addMessage(){
        FacesMessage msg = new FacesMessage("Saved");
        FacesContext.getCurrentInstance().addMessage("success", msg);
    }
    
}
