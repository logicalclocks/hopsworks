package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class Samples implements Serializable {

    public Samples() {
    }
    private boolean materialTypeWholeBlood;
    private boolean materialTypePlasma;
    private boolean materialTypeSerum;
    private boolean materialTypeUrine;
    private boolean materialTypeSaliva;
    private boolean materialTypeCSF;
    private boolean materialTypeDNA;
    private boolean materialTypeRNA;
    private boolean materialTypeCell;
    private boolean materialTypeTissue;
    private boolean materialTypeFaeces;
    private boolean materialTypeOther;
    private String materialTypeOtherText;
    private String cellOrTissueType;
    private boolean temperatureRoom;
    private boolean temperatureP4C;
    private boolean temperatureM18CToM35C;
    private boolean temperatureM60CToM85C;
    private boolean temperatureLiquidNitrogen;
    private boolean temperatureOther;
    private String temperatureOtherText;

    public String getMaterialTypeAll() {
        String all = "";
        all += materialTypeWholeBlood ? "@Whole Blood" : "";
        all += materialTypePlasma ? "@Plasma" : "";
        all += materialTypeSerum ? "@Serum" : "";
        all += materialTypeUrine ? "@Urine" : "";
        all += materialTypeSaliva ? "@Saliva" : "";
        all += materialTypeCSF ? "@CSF" : "";
        all += materialTypeDNA ? "@DNA" : "";
        all += materialTypeRNA ? "@RNA" : "";
        all += materialTypeCell ? "@Cell" : "";
        all += materialTypeTissue ? "@Tissue" : "";
        all += materialTypeFaeces ? "@Faeces" : "";
        all += materialTypeOther ? "@Other" : "";
        all = all.length() > 0 ? all.substring(1) : all;
        return all;
    }

    public boolean isMaterialTypeWholeBlood() {
        return materialTypeWholeBlood;
    }

    public void setMaterialTypeWholeBlood(boolean materialTypeWholeBlood) {
        this.materialTypeWholeBlood = materialTypeWholeBlood;
    }

    public boolean isMaterialTypePlasma() {
        return materialTypePlasma;
    }

    public void setMaterialTypePlasma(boolean materialTypePlasma) {
        this.materialTypePlasma = materialTypePlasma;
    }

    public boolean isMaterialTypeSerum() {
        return materialTypeSerum;
    }

    public void setMaterialTypeSerum(boolean materialTypeSerum) {
        this.materialTypeSerum = materialTypeSerum;
    }

    public boolean isMaterialTypeUrine() {
        return materialTypeUrine;
    }

    public void setMaterialTypeUrine(boolean materialTypeUrine) {
        this.materialTypeUrine = materialTypeUrine;
    }

    public boolean isMaterialTypeSaliva() {
        return materialTypeSaliva;
    }

    public void setMaterialTypeSaliva(boolean materialTypeSaliva) {
        this.materialTypeSaliva = materialTypeSaliva;
    }

    public boolean isMaterialTypeCSF() {
        return materialTypeCSF;
    }

    public void setMaterialTypeCSF(boolean materialTypeCSF) {
        this.materialTypeCSF = materialTypeCSF;
    }

    public boolean isMaterialTypeDNA() {
        return materialTypeDNA;
    }

    public void setMaterialTypeDNA(boolean materialTypeDNA) {
        this.materialTypeDNA = materialTypeDNA;
    }

    public boolean isMaterialTypeRNA() {
        return materialTypeRNA;
    }

    public void setMaterialTypeRNA(boolean materialTypeRNA) {
        this.materialTypeRNA = materialTypeRNA;
    }

    public boolean isMaterialTypeCell() {
        return materialTypeCell;
    }

    public void setMaterialTypeCell(boolean materialTypeCell) {
        this.materialTypeCell = materialTypeCell;
    }

    public boolean isMaterialTypeTissue() {
        return materialTypeTissue;
    }

    public void setMaterialTypeTissue(boolean materialTypeTissue) {
        this.materialTypeTissue = materialTypeTissue;
    }

    public boolean isMaterialTypeFaeces() {
        return materialTypeFaeces;
    }

    public void setMaterialTypeFaeces(boolean materialTypeFaeces) {
        this.materialTypeFaeces = materialTypeFaeces;
    }

    public boolean isMaterialTypeOther() {
        return materialTypeOther;
    }

    public void setMaterialTypeOther(boolean materialTypeOther) {
        this.materialTypeOther = materialTypeOther;
    }

    public String getMaterialTypeOtherText() {
        return materialTypeOtherText;
    }

    public void setMaterialTypeOtherText(String materialTypeOtherText) {
        this.materialTypeOtherText = materialTypeOtherText;
    }

    public String getCellOrTissueType() {
        return cellOrTissueType;
    }

    public void setCellOrTissueType(String cellOrTissueType) {
        this.cellOrTissueType = cellOrTissueType;
    }

    public boolean isTemperatureRoom() {
        return temperatureRoom;
    }

    public void setTemperatureRoom(boolean temperatureRoom) {
        this.temperatureRoom = temperatureRoom;
    }

    public boolean isTemperatureP4C() {
        return temperatureP4C;
    }

    public void setTemperatureP4C(boolean temperatureP4C) {
        this.temperatureP4C = temperatureP4C;
    }

    public boolean isTemperatureM18CToM35C() {
        return temperatureM18CToM35C;
    }

    public void setTemperatureM18CToM35C(boolean temperatureM18CToM35C) {
        this.temperatureM18CToM35C = temperatureM18CToM35C;
    }

    public boolean isTemperatureM60CToM85C() {
        return temperatureM60CToM85C;
    }

    public void setTemperatureM60CToM85C(boolean temperatureM60CToM85C) {
        this.temperatureM60CToM85C = temperatureM60CToM85C;
    }

    public boolean isTemperatureLiquidNitrogen() {
        return temperatureLiquidNitrogen;
    }

    public void setTemperatureLiquidNitrogen(boolean temperatureLiquidNitrogen) {
        this.temperatureLiquidNitrogen = temperatureLiquidNitrogen;
    }

    public boolean isTemperatureOther() {
        return temperatureOther;
    }

    public void setTemperatureOther(boolean temperatureOther) {
        this.temperatureOther = temperatureOther;
    }

    public String getTemperatureOtherText() {
        return temperatureOtherText;
    }

    public void setTemperatureOtherText(String temperatureOtherText) {
        this.temperatureOtherText = temperatureOtherText;
    }
}
