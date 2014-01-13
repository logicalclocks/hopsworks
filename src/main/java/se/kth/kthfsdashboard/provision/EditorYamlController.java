/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import java.io.Serializable;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import se.kth.kthfsdashboard.virtualization.clusterparser.ClusterEntity;
import se.kth.kthfsdashboard.virtualization.clusterparser.ClusterFacade;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@SessionScoped
public class EditorYamlController implements Serializable {

    private static final long serialVersionUID = 20131126L;
    private ClusterEntity entity;
    private String content;
    private String mode = "yaml";
    @EJB
    private ClusterFacade clusterEJB;

    public EditorYamlController() {
    }

    
    public void init() {
        this.content = entity.getYamlContent();
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(final String mode) {
        this.mode = mode;
    }

    public ClusterEntity getEntity() {
        return entity;
    }

    public void setEntity(ClusterEntity entity) {
        this.entity = entity;
    }
    
    public String saveChanges(){
        entity.setYamlContent(content);
        clusterEJB.updateCluster(entity);
        return "editcluster";
    }
}
