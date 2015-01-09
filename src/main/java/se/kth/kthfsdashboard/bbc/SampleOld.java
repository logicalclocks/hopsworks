package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@Entity
@Table(name = "Samples")
@NamedQueries({
   @NamedQuery(name = "Samples.findAll", query = "SELECT s FROM Sample s"),
})
public class SampleOld implements Serializable {

    public Long getId() {
        return id;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }
    
    public String getLastUpdatedFormatted() {
//        return FormatUtils.dateNoTime(lastUpdated);
      return "";
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getSwedishName() {
        return swedishName;
    }

    public void setSwedishName(String swedishName) {
        this.swedishName = swedishName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOtherInformation() {
        return otherInformation;
    }

    public void setOtherInformation(String otherInformation) {
        this.otherInformation = otherInformation;
    }

    public ContactPerson getDataAdminstrator() {
        return dataAdminstrator;
    }

    public void setDataAdminstrator(ContactPerson dataAdminstrator) {
        this.dataAdminstrator = dataAdminstrator;
    }

    public SampleCollection getSampleCollection() {
        return sampleCollection;
    }

    public void setSampleCollection(SampleCollection sampleCollection) {
        this.sampleCollection = sampleCollection;
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(Diagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public Samples getSamples() {
        return samples;
    }

    public void setSamples(Samples samples) {
        this.samples = samples;
    }

    public SampleDonors getSampleDonors() {
        return sampleDonors;
    }

    public void setSampleDonors(SampleDonors sampleDonors) {
        this.sampleDonors = sampleDonors;
    }

   public enum Provider {
      Collectd, Agent
   }   
   
   @Id
   @GeneratedValue(strategy = GenerationType.SEQUENCE)
   private Long id;
   
   @Temporal(javax.persistence.TemporalType.TIMESTAMP)
   private Date lastUpdated;
   
   @Column(length = 512)
   private String swedishName;
   @Column(length = 512)
   private String englishName;
   @Column(length = 512)   
   private String responsible;
   @Column(length = 512)   
   private String organization;
   @Column(length = 1024)   
   private String description;
   @Column(length = 1024)   
   private String otherInformation;

   private ContactPerson dataAdminstrator;
   private SampleCollection sampleCollection;
   private Study study;
   private Diagnosis diagnosis;
   private Samples samples;
   private SampleDonors sampleDonors;
   
   public SampleOld() {
   }


}