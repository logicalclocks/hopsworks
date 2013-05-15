package se.kth.kthfsdashboard.command;

  
import java.io.Serializable;  
  
import javax.faces.application.FacesMessage;  
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;  

@ManagedBean
@SessionScoped
//@RequestScoped
public class ProgressBean implements Serializable {  
  
    private Integer progress;  
  
    public Integer getProgress() {  
       
       System.err.println(">>>>>>>>>>" +progress);
       
//       if (progress == 100) {
//          progress = null;
//       }
               
        if(progress == null)  
            progress = 0;  
        else {  
            progress = progress + (int)(Math.random() * 20);  
              
            if(progress > 100)  
                progress = 100;  
        }  
          
        return progress;  
    }  
  
    public void setProgress(Integer progress) {  
       System.err.println("---------" +progress);
       this.progress = progress;  
    }  
      
    public void onComplete() {  
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Progress Completed", "Progress Completed"));  
    }  
      
    public void cancel() {  
        progress = null;  
    }  
}  