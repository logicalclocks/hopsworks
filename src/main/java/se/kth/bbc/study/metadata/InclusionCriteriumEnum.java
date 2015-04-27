package se.kth.bbc.study.metadata;

/**
 *
 * @author stig
 */
public enum InclusionCriteriumEnum {

  HEALTH_STATUS,
  HOSPITAL_PATIENT,
  MEDICATION_USE,
  GRAVIDITY,
  AGE,
  GROUP,
  FAMILIAL_STATUS,
  SEX,
  COUNTRY_RESIDENCE,
  ETHNIC_ORIGIN,
  POPULATION_REPRESENTATIVE,
  LIFESTYLE,
  OTHER;

  @Override
  public String toString() {
    switch (this) {
      case HEALTH_STATUS:
        return "Health status";
      case HOSPITAL_PATIENT:
        return "Hospital patient";
      case MEDICATION_USE:
        return "Use of medication";
      case GRAVIDITY:
        return "Gravidity";
      case AGE:
        return "Age";
      case GROUP:
        return "Group";
      case FAMILIAL_STATUS:
        return "Familial status";
      case SEX:
        return "Sex";
      case COUNTRY_RESIDENCE:
        return "Country of residence";
      case ETHNIC_ORIGIN:
        return "Ethnic origin";
      case POPULATION_REPRESENTATIVE:
        return "Population representative sampling";
      case LIFESTYLE:
        return "Lifestyle/exposure";
      case OTHER:
        return "Other";
      default:
        throw new IllegalStateException("Illegal enum value.");
    }
  }
}
