package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import org.primefaces.context.RequestContext;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@SessionScoped
public class BbcController implements Serializable {

    
    private String testValue;
    
    public BbcController() {
    }

    public void showNewSampleCollectionDialog() {
        System.out.println("HERE");
        RequestContext.getCurrentInstance().update("formNewSampleCollection");
        RequestContext.getCurrentInstance().reset("formNewSampleCollection");
        RequestContext.getCurrentInstance().execute("dlgNewSampleCollection.show()");
    }
    
    public void showFindSampleCollectionDialog() {
//        RequestContext.getCurrentInstance().update("formFindSampleCollection");
//        RequestContext.getCurrentInstance().reset("formFindSampleCollection");
//        RequestContext.getCurrentInstance().execute("dlgFindSampleCollection.show()");
    }    
    
    public String getTestValue() {
        return testValue;
    }

    public void setTestValue(String testValue) {
        this.testValue = testValue;
    }
}
