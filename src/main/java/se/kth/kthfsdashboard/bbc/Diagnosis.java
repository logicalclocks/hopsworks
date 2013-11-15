package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class Diagnosis implements Serializable{
    
    public Diagnosis() {
        
    }
    
    private String diagnosisGroup;
    private boolean comorbidity;

    public String getDiagnosisGroup() {
        return diagnosisGroup;
    }

    public void setDiagnosisGroup(String diagnosisGroup) {
        this.diagnosisGroup = diagnosisGroup;
    }

    public boolean isComorbidity() {
        return comorbidity;
    }

    public void setComorbidity(boolean comorbidity) {
        this.comorbidity = comorbidity;
    }
    
    
}
