package se.kth.kthfsdashboard.bbc;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
@ManagedBean
@RequestScoped
public class StaticDataController implements Serializable {

    private final static List<String> organizations = new ArrayList<String>();
    private final static List<String> diagnosisGroups = new ArrayList<String>();
    private final static ContactPerson dataAdministrator;

    static {
        organizations.add("Göteborgs Universitet");
        organizations.add("Hälsohögskolan i Jönköping");
        organizations.add("Infrastructure BIOBANQUES - US013 INSERM");
        organizations.add("KTH");
        organizations.add("Karolinska Institutet");
        organizations.add("Landstinget Dalarna");
        organizations.add("Landstinget i Kalmar län");
        organizations.add("Landstinget i Uppsala län");
        organizations.add("Landstinget i Värmland");
        organizations.add("Landstinget i Östergötland");
        organizations.add("Linköpings universitet");
        organizations.add("Lunds Universitet");
        organizations.add("Norrbottens Läns Landsting");
        organizations.add("Region Skåne");
        organizations.add("Stockholms Läns Landsting");
        organizations.add("Swedish University of Agricultural Sciences");
        organizations.add("Umeå Universitet");
        organizations.add("Uppsala Universitet");

        diagnosisGroups.add("Certain infectious and parasitic diseases A00-B99");
        diagnosisGroups.add("Neoplasms C00-D48");
        diagnosisGroups.add("Diseases of the blood and blood-forming organs and certain disorders involving the immune mechanism D50-D89");
        diagnosisGroups.add("Endocrine, nutritional and metabolic diseases E00-E90");
        diagnosisGroups.add("Mental and behavioural disorders F00-F99");
        diagnosisGroups.add("Diseases of the nervous system G00-G99");
        diagnosisGroups.add("Diseases of the eye and adnexa H00-H59");
        diagnosisGroups.add("Diseases of the ear and mastoid process H60-H95");
        diagnosisGroups.add("Diseases of the circulatory system I00-I99");
        diagnosisGroups.add("Diseases of the respiratory system J00-J99");
        diagnosisGroups.add("Diseases of the digestive system K00-K93");
        diagnosisGroups.add("Diseases of the skin and subcutaneous tissue L00-L99");
        diagnosisGroups.add("Diseases of the musculoskeletal system and connective tissue M00-M99");
        diagnosisGroups.add("Diseases of the genitourinary system N00-N99");
        diagnosisGroups.add("Pregnancy, childbirth and the puerperium O00-O99");
        diagnosisGroups.add("Certain conditions originating in the perinatal period P00-P96");
        diagnosisGroups.add("Congenital malformations, deformations and chromosomal abnormalities Q00-Q99");
        diagnosisGroups.add("Symptoms, signs and abnormal clinical and laboratory findings, not elsewhere classified R00-R99");
        diagnosisGroups.add("Injury, poisoning and certain other consequences of external causes S00-T98");
        diagnosisGroups.add("External causes of morbidity and mortality V01-Y98");
        diagnosisGroups.add("Factors influencing health status and contact with health services Z00-Z99");
        diagnosisGroups.add("Codes for special purposes U00-U89");

        dataAdministrator = new ContactPerson();
        dataAdministrator.setFirstName("Jim");
        dataAdministrator.setLastName("Dowling");
        dataAdministrator.setEmailAddress("jdowling@sics.se");
        dataAdministrator.setPhone("");
        dataAdministrator.setStreetName("ISAFJORDSGATAN 39");
        dataAdministrator.setZip("16440");
        dataAdministrator.setCity("Kista");
        dataAdministrator.setCountry("Sweden");
        dataAdministrator.setOrganization("KTH");
        dataAdministrator.setDepartment("ICT");
        dataAdministrator.setAddress("ISAFJORDSGATAN 39, 16440 Kista, Sweden");
    }

    public StaticDataController() {
    }

    public List<String> getOrganizations() {
//      TODO: read from database
        return organizations;
    }

    public List<String> getDiagnosisGroups() {
//      TODO: read from database
        return diagnosisGroups;
    }

    public static ContactPerson getDataAdministrator() {
//      TODO: read from database
        return dataAdministrator;
    }
}
