package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;
import java.util.Date;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import org.primefaces.context.RequestContext;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class SampleController implements Serializable {

    @EJB
    private SampleEjb sampleEjb;
    private Sample sample = new Sample();
    private boolean create;
    private boolean show;
    
    public SampleController() {

        sample.setLastUpdated(new Date());
        sample.setDataAdminstrator(StaticDataController.getDataAdministrator());
        sample.setSampleCollection(new SampleCollection());
        sample.setStudy(new Study());
        sample.setDiagnosis(new Diagnosis());
        sample.setSamples(new Samples());
        sample.setSampleDonors(new SampleDonors());
    }
    
    public void createSample() {
        sampleEjb.persistSample(sample);
        RequestContext.getCurrentInstance().execute("dlgNewSampleCollection.hide()");        
        System.out.println("*** Created: " + sample.getSwedishName());        
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        System.out.println("HERE");
        this.sample = sample;
    }

    public boolean isCreate() {
        return create;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean isShow() {
        return show;
    }

    public void setShow(boolean show) {
        this.show = show;
    }
}
