package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class Study implements Serializable {
    
    public Study() {
    }
    
    private String aim;
    private boolean typeOfStudyCaseControl;
    private boolean typeOfStudyCohort;
    private boolean typeOfStudyLongitudinal;
    private boolean typeOfStudyTwinStudy;
    private boolean typeOfStudyQualityControl;
    private boolean typeOfStudyPopulationbased;
    private boolean typeOfStudyDiseaseSpecific;
    private boolean typeOfStudyCrossSectional;
    private boolean typeOfStudyOther;
    private String typeOfStudyOtherText;
    private Date start;
    private String plannedSampledIndividuals;
    private boolean omicsExperimentsExists;
    private boolean omicsExperimentsPlanned;
    private boolean TypeOfOmicsGenomics;
    private boolean TypeOfOmicsTranscriptomics;
    private boolean TypeOfOmicsProteomics;
    private boolean TypeOfOmicsMetabolomics;
    private boolean TypeOfOmicsLipidomics;
    private boolean TypeOfOmicsOther;
    private String omicsMethod;

    public String getAim() {
        return aim;
    }

    public void setAim(String aim) {
        this.aim = aim;
    }

    public boolean isTypeOfStudyCaseControl() {
        return typeOfStudyCaseControl;
    }

    public void setTypeOfStudyCaseControl(boolean typeOfStudyCaseControl) {
        this.typeOfStudyCaseControl = typeOfStudyCaseControl;
    }

    public boolean isTypeOfStudyCohort() {
        return typeOfStudyCohort;
    }

    public void setTypeOfStudyCohort(boolean typeOfStudyCohort) {
        this.typeOfStudyCohort = typeOfStudyCohort;
    }

    public boolean isTypeOfStudyLongitudinal() {
        return typeOfStudyLongitudinal;
    }

    public void setTypeOfStudyLongitudinal(boolean typeOfStudyLongitudinal) {
        this.typeOfStudyLongitudinal = typeOfStudyLongitudinal;
    }

    public boolean isTypeOfStudyTwinStudy() {
        return typeOfStudyTwinStudy;
    }

    public void setTypeOfStudyTwinStudy(boolean typeOfStudyTwinStudy) {
        this.typeOfStudyTwinStudy = typeOfStudyTwinStudy;
    }

    public boolean isTypeOfStudyQualityControl() {
        return typeOfStudyQualityControl;
    }

    public void setTypeOfStudyQualityControl(boolean typeOfStudyQualityControl) {
        this.typeOfStudyQualityControl = typeOfStudyQualityControl;
    }

    public boolean isTypeOfStudyPopulationbased() {
        return typeOfStudyPopulationbased;
    }

    public void setTypeOfStudyPopulationbased(boolean typeOfStudyPopulationbased) {
        this.typeOfStudyPopulationbased = typeOfStudyPopulationbased;
    }

    public boolean isTypeOfStudyDiseaseSpecific() {
        return typeOfStudyDiseaseSpecific;
    }

    public void setTypeOfStudyDiseaseSpecific(boolean typeOfStudyDiseaseSpecific) {
        this.typeOfStudyDiseaseSpecific = typeOfStudyDiseaseSpecific;
    }

    public boolean isTypeOfStudyCrossSectional() {
        return typeOfStudyCrossSectional;
    }

    public void setTypeOfStudyCrossSectional(boolean typeOfStudyCrossSectional) {
        this.typeOfStudyCrossSectional = typeOfStudyCrossSectional;
    }

    public boolean isTypeOfStudyOther() {
        return typeOfStudyOther;
    }

    public void setTypeOfStudyOther(boolean typeOfStudyOther) {
        this.typeOfStudyOther = typeOfStudyOther;
    }

    public String getTypeOfStudyOtherText() {
        return typeOfStudyOtherText;
    }

    public void setTypeOfStudyOtherText(String typeOfStudyOtherText) {
        this.typeOfStudyOtherText = typeOfStudyOtherText;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public String getPlannedSampledIndividuals() {
        return plannedSampledIndividuals;
    }

    public void setPlannedSampledIndividuals(String plannedSampledIndividuals) {
        this.plannedSampledIndividuals = plannedSampledIndividuals;
    }

    public boolean isOmicsExperimentsExists() {
        return omicsExperimentsExists;
    }

    public void setOmicsExperimentsExists(boolean omicsExperimentsExists) {
        this.omicsExperimentsExists = omicsExperimentsExists;
    }

    public boolean isOmicsExperimentsPlanned() {
        return omicsExperimentsPlanned;
    }

    public void setOmicsExperimentsPlanned(boolean omicsExperimentsPlanned) {
        this.omicsExperimentsPlanned = omicsExperimentsPlanned;
    }

    public boolean isTypeOfOmicsGenomics() {
        return TypeOfOmicsGenomics;
    }

    public void setTypeOfOmicsGenomics(boolean TypeOfOmicsGenomics) {
        this.TypeOfOmicsGenomics = TypeOfOmicsGenomics;
    }

    public boolean isTypeOfOmicsTranscriptomics() {
        return TypeOfOmicsTranscriptomics;
    }

    public void setTypeOfOmicsTranscriptomics(boolean TypeOfOmicsTranscriptomics) {
        this.TypeOfOmicsTranscriptomics = TypeOfOmicsTranscriptomics;
    }

    public boolean isTypeOfOmicsProteomics() {
        return TypeOfOmicsProteomics;
    }

    public void setTypeOfOmicsProteomics(boolean TypeOfOmicsProteomics) {
        this.TypeOfOmicsProteomics = TypeOfOmicsProteomics;
    }

    public boolean isTypeOfOmicsMetabolomics() {
        return TypeOfOmicsMetabolomics;
    }

    public void setTypeOfOmicsMetabolomics(boolean TypeOfOmicsMetabolomics) {
        this.TypeOfOmicsMetabolomics = TypeOfOmicsMetabolomics;
    }

    public boolean isTypeOfOmicsLipidomics() {
        return TypeOfOmicsLipidomics;
    }

    public void setTypeOfOmicsLipidomics(boolean TypeOfOmicsLipidomics) {
        this.TypeOfOmicsLipidomics = TypeOfOmicsLipidomics;
    }

    public boolean isTypeOfOmicsOther() {
        return TypeOfOmicsOther;
    }

    public void setTypeOfOmicsOther(boolean TypeOfOmicsOther) {
        this.TypeOfOmicsOther = TypeOfOmicsOther;
    }

    public String getOmicsMethod() {
        return omicsMethod;
    }

    public void setOmicsMethod(String omicsMethod) {
        this.omicsMethod = omicsMethod;
    }
        
}
