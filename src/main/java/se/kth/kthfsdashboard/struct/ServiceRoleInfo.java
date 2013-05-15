package se.kth.kthfsdashboard.struct;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ServiceRoleInfo {

    private String name;
    private String shortName;
    private String statusStarted;
    private String statusStopped;
    private String health;


    public ServiceRoleInfo (String name, String shortName) {
        this.name = name;
        this.shortName = shortName;
    }
            
    public ServiceRoleInfo (String name, String shortName, String statusStarted, String statusStopped, String health) {
        this.name = name;
        this.shortName = shortName;
        this.statusStarted = statusStarted;
        this.statusStopped = statusStopped;
        this.health = health;
                
    }
    
    public String getName() {
        return name;
    }
    
    public String getShortName(){
        return shortName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatusStarted() {
        return statusStarted;
    }

    public void setStatusStarted(String statusStarted) {
        this.statusStarted = statusStarted;
    }

    public String getStatusStopped() {
        return statusStopped;
    }

    public void setStatusStopped(String statusStopped) {
        this.statusStopped = statusStopped;
    }

    public String getHealth() {
        return health;
    }

    public void setHealth(String health) {
        this.health = health;
    }


}