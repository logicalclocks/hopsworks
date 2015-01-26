package se.kth.bbc.study.samples;

/**
 *
 * @author stig
 */
public enum MaterialTypeEnum {

  DNA("A long linear double-stranded polymer formed from nucleotides attached to a deoxyribose backbone and found in the nucleus of a cell; associated with the transmission of genetic information."),
  CDNA_MRNA("Single-stranded DNA that is complementary to messenger RNA or DNA that has been synthesized from messenger RNA by reverse transcriptase/A class of RNA molecule containing protein-coding information in its nucleotide sequence that can be translated into the amino acid sequence of a protein."),
  MICRORNA("A type of RNA found in cells and in blood. MicroRNAs are smaller than many other types of RNA and can bind to messenger RNAs (mRNAs) to block them from making proteins. MicroRNAs are being studied in the diagnosis (NCI) and treatment of cancer."),
  WHOLE_BLOOD("Blood that has not been separated into its various components; blood that has not been modified except for the addition of an anticoagulant."),
  PERIPHERAL_BLOOD_CELLS("A general term describing the three cellular components of blood (white blood cells, red blood cells, and platelets), all which are made in the bone marrow."),
  PLASMA("Plasma is the fluid (acellular) portion of the circulating blood, as distinguished from the serum that is the fluid portion of the blood obtained by removal of the fibrin clot and blood cells after coagulation. 	"),
  SERUM("The clear portion of the blood that remains after the removal of the blood cells and the clotting proteins."),
  TISSUE_CRYO("An anatomical structure consisting of similarly specialized cells and intercellular matrix, aggregated according to genetically determined spatial relationships, performing a specific function. Preserved by freezing in liquid nitrogen"),
  TISSUE_PARAFFIN("Tissue that is preserved and embedded in paraffin."),
  CELL_LINES("Cells of a single type (human, animal, or plant) that have been adapted to grow continuously in the laboratory and are used in research."),
  URINE("The fluid that is excreted by the kidneys. It is stored in the bladder and discharged through the urethra."),
  SALIVA("A clear liquid secreted into the mouth by the salivary glands and mucous glands of the mouth; moistens the mouth and starts the digestion of starches."),
  FAECES("The material discharged from the bowel during defecation. It consists of undigested food, intestinal mucus, epithelial cells, and bacteria."),
  PATHOGEN("A biological agent causing disease; a disease producer e.g. virus, bacterium, prion, other microorganism etc."),
  OTHER("Any other type of material taken from a biological entity, e.g. amniotic fluid, cerebrospinal fluid (CSV), mitochondrial RNA.");

  private final String definition;

  public String definition() {
    return definition;
  }

  MaterialTypeEnum(String def) {
    this.definition = def;
  }
  
  @Override
  public String toString(){
    switch(this){
      case DNA:
        return "DNA";
      case CDNA_MRNA:
        return "cDNA/mRNA";
      case MICRORNA:
        return "microRNA";
      case WHOLE_BLOOD:
        return "Whole blood";
      case PERIPHERAL_BLOOD_CELLS:
        return "Peripheral blood cells";
      case PLASMA:
        return "Plasma";
      case SERUM:
        return "Serum";
      case TISSUE_CRYO:
        return "Tissue, cryo preserved";
      case TISSUE_PARAFFIN:
        return "Tissue, paraffin preserved";
      case CELL_LINES:
        return "Cell lines";
      case URINE:
        return "Urine";
      case SALIVA:
        return "Saliva";
      case FAECES:
        return "Faeces";
      case PATHOGEN:
        return "Pathogen";
      case OTHER:
        return "Other";
      default:
        throw new IllegalStateException("Illegal enum value.");
    }
  }
}
