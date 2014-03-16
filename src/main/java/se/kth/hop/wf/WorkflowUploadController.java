/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.wf;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
@ManagedBean
@RequestScoped
public class WorkflowUploadController implements Serializable{

    @EJB
    private WorkflowFacade workflowFacade;
    private UploadedFile file;
    private String tags;
    private String owner;
    
    public UploadedFile getFile() {
        return file;
    }
    
    public void setFile(UploadedFile file) {
        this.file = file;
    }
    
    public String getTags() {
        return tags;
    }
    
    public void setTags(String tags) {
        this.tags = tags;
    }
    
    public String getOwner() {
        return owner;
    }
    
    public void setOwner(String owner) {
        this.owner = owner;
    }
    
    public String addNewWorkflow() {
        try {
            System.out.println(file!=null);
            byte[] data = file.getContents();
            String content = new String(data, "UTF-8");
            Workflow created = new Workflow(owner, file.getFileName(), new Date().toString(), tags, content);
            workflowFacade.create(created);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unsupported format");
        }
        return "manageWorkflows";
    }
}
