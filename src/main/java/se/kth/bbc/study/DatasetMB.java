/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package se.kth.bbc.study;

import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author roshan
 */
@ManagedBean
@SessionScoped
public class DatasetMB implements Serializable{
    
    @EJB
    private DatasetController datasetController;
    private Dataset dataset;
    
    private String owner;
    private String datasetName;
    
    
    public String getOwner(){
        return owner;
    }
    
    public void setOwner(String owner){
        this.owner = owner;
    }
    
    public String getDatasetName(){
        return datasetName;
    }
    
    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }
    
    public Dataset getDataset() {
        if (dataset == null) {
            dataset = new Dataset();
        }
        return dataset;
    }
    
    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    } 
        
    public String getDatasetOwner(){
        return getUsername();
    }
    
    
    public String createDataset(){
        
             
        dataset.setId(Integer.SIZE);     
        dataset.setOwner(getUsername());
      
        try{
            datasetController.persistDataset(dataset);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Dataset wasn't created.");
            return null;
        }
        addMessage("Dataset created.");
        return "Success!";
    }
    
    public String deleteDataset(){
        try{
            datasetController.removeDataset(dataset);
        }catch (EJBException ejb) {
            addErrorMessageToUserAction("Error: Dataset wasn't removed.");
            return null;
        }
        addMessage("Dataset removed.");
        return "Success!";
    }
    
    
    public String fetchOwner(){
    
        FacesContext fc = FacesContext.getCurrentInstance();
        Map<String,String> params = fc.getExternalContext().getRequestParameterMap();
        this.owner =  params.get("owner"); 
//        this.datasetName =  params.get("datasetName"); 
        createDataset();
        
        return "dataUpload";
    
    }
    
    
    private HttpServletRequest getRequest() {
        return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    }
    
    
    public String getUsername(){
          return getRequest().getUserPrincipal().getName();
    }
   
    public void addMessage(String summary) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, summary, summary);
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void addErrorMessageToUserAction(String message) {
        FacesMessage errorMessage = new FacesMessage(FacesMessage.SEVERITY_ERROR, message, message);
        FacesContext.getCurrentInstance().addMessage(null, errorMessage);
    }
    
    
    
}
