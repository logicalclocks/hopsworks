package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;
import java.util.List;
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
public class SearchController implements Serializable {

    @EJB
    private SampleEjb sampleEjb;
    private Sample sample = new Sample();
    private List<Sample> samples;
    private boolean showSamples;
    
    public SearchController() {

        sample.setSampleCollection(new SampleCollection());
        sample.setStudy(new Study());
        sample.setDiagnosis(new Diagnosis());
        sample.setSamples(new Samples());
        sample.setSampleDonors(new SampleDonors());

    }

    public void searchSample() {
        System.out.println("Searching...");
        showSamples = true;        
        RequestContext.getCurrentInstance().execute("dlgFindSampleCollection.hide()");
    }

    public Sample getSample() {        
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public List<Sample> getSamples() {
        samples = sampleEjb.findAll();        
        System.out.println("Found " + samples.size() + " samples");        
        return samples;
    }

    public boolean isShowSamples() {
        return showSamples;
    }
}
