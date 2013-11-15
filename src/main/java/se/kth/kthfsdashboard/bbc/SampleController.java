package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;
import java.util.Date;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

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
        System.out.println("*** " + sample.getLastUpdatedFormatted() + " " + sample.getSwedishName());
        sampleEjb.persistSample(sample);
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }
}
