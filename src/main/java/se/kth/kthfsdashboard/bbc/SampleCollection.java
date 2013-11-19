package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Temporal;

/**
 *
 * @author Hamidreza Afzali <afzalli@kth.se>
 */
public class SampleCollection implements Serializable {

    public SampleCollection() {
    }
    private boolean consistsOfHealthCare;
    private boolean consistsOfOtherResearch;
    private boolean consistsOfThisResearch;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date starts;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date ends;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    private Date destruction;
    private boolean categoryRegisterData;
    private boolean categorySurvey;
    private boolean categoryPhysiologicalMeasurements;
    private boolean categoryImagingData;
    private boolean categoryMedicalRecords;
    private boolean categoryRegisterOther;
    private String categoryOtherText;
    private boolean dataIndividualDiseaseHistory;
    private boolean dataIndividualHistoryOfInjuries;
    private boolean dataMedication;
    private boolean dataPerceptionOfHealth;
    private boolean dataWomensHealth;
    private boolean dataReproductiveHistory;
    private boolean dataFamilialDiseaseHistory;
    private boolean dataLifeHabitsBehaviours;
    private boolean dataSociodemographicCharacteristics;
    private boolean dataSocioeconomicCharacteristics;
    private boolean dataPhysicalEnvironment;
    private boolean dataMentalHealth;
    private boolean dataOther;
    private String dataOtherText;

    public String getCategoryAll() {
        String all = "";
        all += categoryRegisterData ? "@Register Data" : "";
        all += categorySurvey ? "@Survey" : "";
        all += categoryPhysiologicalMeasurements ? "@Physiological Measurements" : "";
        all += categoryImagingData ? "@Imaging Data" : "";
        all += categoryMedicalRecords ? "@Medical Records" : "";
        all += categoryRegisterOther ? "@Register Other" : "";
        all = all.length() > 0 ? all.substring(1) : all;
        return all;
    }

    public String getDataAll() {
        String all = "";
        all += dataIndividualDiseaseHistory ? "@Individual Disease History" : "";
        all += dataIndividualHistoryOfInjuries ? "@Individual History of Injuries" : "";
        all += dataMedication ? "@Medication" : "";
        all += dataPerceptionOfHealth ? "@Perception of Health" : "";
        all += dataWomensHealth ? "@Womens Health" : "";
        all += dataReproductiveHistory ? "@Reproductive History" : "";
        all += dataFamilialDiseaseHistory ? "@Familial Disease History" : "";
        all += dataLifeHabitsBehaviours ? "@Life Habits Behaviours" : "";
        all += dataSociodemographicCharacteristics ? "@Sociodemographic Characteristics" : "";
        all += dataSocioeconomicCharacteristics ? "@Socioeconomic Characteristics" : "";
        all += dataPhysicalEnvironment ? "@Physical Environment" : "";
        all += dataMentalHealth ? "@Mental Health" : "";
        all += dataOther ? "@Other" : "";
        all = all.length() > 0 ? all.substring(1) : all;
        return all;
    }

    public boolean isConsistsOfHealthCare() {
        return consistsOfHealthCare;
    }

    public void setConsistsOfHealthCare(boolean consistsOfHealthCare) {
        this.consistsOfHealthCare = consistsOfHealthCare;
    }

    public boolean isConsistsOfOtherResearch() {
        return consistsOfOtherResearch;
    }

    public void setConsistsOfOtherResearch(boolean consistsOfOtherResearch) {
        this.consistsOfOtherResearch = consistsOfOtherResearch;
    }

    public boolean isConsistsOfThisResearch() {
        return consistsOfThisResearch;
    }

    public void setConsistsOfThisResearch(boolean consistsOfThisResearch) {
        this.consistsOfThisResearch = consistsOfThisResearch;
    }

    public Date getStarts() {
        return starts;
    }

    public void setStarts(Date starts) {
        this.starts = starts;
    }

    public Date getEnds() {
        return ends;
    }

    public void setEnds(Date ends) {
        this.ends = ends;
    }

    public Date getDestruction() {
        return destruction;
    }

    public void setDestruction(Date destruction) {
        this.destruction = destruction;
    }

    public boolean isCategoryRegisterData() {
        return categoryRegisterData;
    }

    public void setCategoryRegisterData(boolean categoryRegisterData) {
        this.categoryRegisterData = categoryRegisterData;
    }

    public boolean isCategorySurvey() {
        return categorySurvey;
    }

    public void setCategorySurvey(boolean categorySurvey) {
        this.categorySurvey = categorySurvey;
    }

    public boolean isCategoryPhysiologicalMeasurements() {
        return categoryPhysiologicalMeasurements;
    }

    public void setCategoryPhysiologicalMeasurements(boolean categoryPhysiologicalMeasurements) {
        this.categoryPhysiologicalMeasurements = categoryPhysiologicalMeasurements;
    }

    public boolean isCategoryImagingData() {
        return categoryImagingData;
    }

    public void setCategoryImagingData(boolean categoryImagingData) {
        this.categoryImagingData = categoryImagingData;
    }

    public boolean isCategoryMedicalRecords() {
        return categoryMedicalRecords;
    }

    public void setCategoryMedicalRecords(boolean categoryMedicalRecords) {
        this.categoryMedicalRecords = categoryMedicalRecords;
    }

    public boolean isCategoryRegisterOther() {
        return categoryRegisterOther;
    }

    public void setCategoryRegisterOther(boolean categoryRegisterOther) {
        this.categoryRegisterOther = categoryRegisterOther;
    }

    public String getCategoryOtherText() {
        return categoryOtherText;
    }

    public void setCategoryOtherText(String categoryOtherText) {
        this.categoryOtherText = categoryOtherText;
    }

    public boolean isDataIndividualDiseaseHistory() {
        return dataIndividualDiseaseHistory;
    }

    public void setDataIndividualDiseaseHistory(boolean dataIndividualDiseaseHistory) {
        this.dataIndividualDiseaseHistory = dataIndividualDiseaseHistory;
    }

    public boolean isDataIndividualHistoryOfInjuries() {
        return dataIndividualHistoryOfInjuries;
    }

    public void setDataIndividualHistoryOfInjuries(boolean dataIndividualHistoryOfInjuries) {
        this.dataIndividualHistoryOfInjuries = dataIndividualHistoryOfInjuries;
    }

    public boolean isDataMedication() {
        return dataMedication;
    }

    public void setDataMedication(boolean dataMedication) {
        this.dataMedication = dataMedication;
    }

    public boolean isDataPerceptionOfHealth() {
        return dataPerceptionOfHealth;
    }

    public void setDataPerceptionOfHealth(boolean dataPerceptionOfHealth) {
        this.dataPerceptionOfHealth = dataPerceptionOfHealth;
    }

    public boolean isDataWomensHealth() {
        return dataWomensHealth;
    }

    public void setDataWomensHealth(boolean dataWomensHealth) {
        this.dataWomensHealth = dataWomensHealth;
    }

    public boolean isDataReproductiveHistory() {
        return dataReproductiveHistory;
    }

    public void setDataReproductiveHistory(boolean dataReproductiveHistory) {
        this.dataReproductiveHistory = dataReproductiveHistory;
    }

    public boolean isDataFamilialDiseaseHistory() {
        return dataFamilialDiseaseHistory;
    }

    public void setDataFamilialDiseaseHistory(boolean dataFamilialDiseaseHistory) {
        this.dataFamilialDiseaseHistory = dataFamilialDiseaseHistory;
    }

    public boolean isDataLifeHabitsBehaviours() {
        return dataLifeHabitsBehaviours;
    }

    public void setDataLifeHabitsBehaviours(boolean dataLifeHabitsBehaviours) {
        this.dataLifeHabitsBehaviours = dataLifeHabitsBehaviours;
    }

    public boolean isDataSociodemographicCharacteristics() {
        return dataSociodemographicCharacteristics;
    }

    public void setDataSociodemographicCharacteristics(boolean dataSociodemographicCharacteristics) {
        this.dataSociodemographicCharacteristics = dataSociodemographicCharacteristics;
    }

    public boolean isDataSocioeconomicCharacteristics() {
        return dataSocioeconomicCharacteristics;
    }

    public void setDataSocioeconomicCharacteristics(boolean dataSocioeconomicCharacteristics) {
        this.dataSocioeconomicCharacteristics = dataSocioeconomicCharacteristics;
    }

    public boolean isDataPhysicalEnvironment() {
        return dataPhysicalEnvironment;
    }

    public void setDataPhysicalEnvironment(boolean dataPhysicalEnvironment) {
        this.dataPhysicalEnvironment = dataPhysicalEnvironment;
    }

    public boolean isDataMentalHealth() {
        return dataMentalHealth;
    }

    public void setDataMentalHealth(boolean dataMentalHealth) {
        this.dataMentalHealth = dataMentalHealth;
    }

    public boolean isDataOther() {
        return dataOther;
    }

    public void setDataOther(boolean dataOther) {
        this.dataOther = dataOther;
    }

    public String getDataOtherText() {
        return dataOtherText;
    }

    public void setDataOtherText(String dataOtherText) {
        this.dataOtherText = dataOtherText;
    }
}
