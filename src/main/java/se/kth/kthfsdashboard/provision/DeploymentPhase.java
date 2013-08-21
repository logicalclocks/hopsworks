/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public enum DeploymentPhase {
    CREATION("Creation"),
    CREATED("Created"),
    INSTALL("Install"),
    CONFIGURE("Configure"),
    WAITING("Waiting"),
    COMPLETE("Complete"),
    ERROR("Error"),
    RETRYING("Retrying");
    
     private final String text;
     
     private DeploymentPhase(final String text){
        this.text=text;
    }
    
    public static DeploymentPhase fromString(String text){
        if(text!=null){
            for(DeploymentPhase value: DeploymentPhase.values()){
                if(text.equals(value.toString())){
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return text;
    }
}
