package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class Diagnosis implements Serializable {

  public Diagnosis() {

  }

  private String diagnosisGroup;
  private Boolean comorbidity;

  public String getDiagnosisGroup() {
    return diagnosisGroup;
  }

  public void setDiagnosisGroup(String diagnosisGroup) {
    this.diagnosisGroup = diagnosisGroup;
  }

  public Boolean getComorbidity() {
    return comorbidity;
  }

  public void setComorbidity(Boolean comorbidity) {
    this.comorbidity = comorbidity;
  }

}
