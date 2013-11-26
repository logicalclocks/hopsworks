/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

import java.io.Serializable;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import se.kth.kthfsdashboard.virtualization.clusterparser.ClusterEntity;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@RequestScoped
public class YAMLEditorController implements Serializable {

    private static final long serialVersionUID = 20131126L;
    private ClusterEntity entity;
    private String content;
    private String mode = "yaml";

    public YAMLEditorController() {
      
    }
    @PostConstruct
    public void init(){
        this.content=entity.getYamlContent();
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
    
}
