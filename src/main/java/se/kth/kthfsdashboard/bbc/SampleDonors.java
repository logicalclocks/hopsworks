package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class SampleDonors implements Serializable {

  public SampleDonors() {
  }
  private boolean sexFemale;
  private boolean sexMale;
  private String numberOfSampleDonors;
  private String ageYoungest;
  private String ageOldest;

  public String getSexAll() {
    String all = "";
    all += sexFemale ? "@Female" : "";
    all += sexMale ? "@Male" : "";
    all = all.length() > 0 ? all.substring(1) : all;
    return all;
  }

  public boolean isSexFemale() {
    return sexFemale;
  }

  public void setSexFemale(boolean sexFemale) {
    this.sexFemale = sexFemale;
  }

  public boolean isSexMale() {
    return sexMale;
  }

  public void setSexMale(boolean sexMale) {
    this.sexMale = sexMale;
  }

  public String getNumberOfSampleDonors() {
    return numberOfSampleDonors;
  }

  public void setNumberOfSampleDonors(String numberOfSampleDonors) {
    this.numberOfSampleDonors = numberOfSampleDonors;
  }

  public String getAgeYoungest() {
    return ageYoungest;
  }

  public void setAgeYoungest(String ageYoungest) {
    this.ageYoungest = ageYoungest;
  }

  public String getAgeOldest() {
    return ageOldest;
  }

  public void setAgeOldest(String ageOldest) {
    this.ageOldest = ageOldest;
  }
}
