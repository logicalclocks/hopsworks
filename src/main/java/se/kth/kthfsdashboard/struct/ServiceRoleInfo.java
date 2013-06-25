package se.kth.kthfsdashboard.struct;

import se.kth.kthfsdashboard.role.RoleType;

/**
 *
 * @author Hamidreza Afzali <afzali@kth.se>
 */
public class ServiceRoleInfo {

    private String fullName;
    private String roleName;
    private String statusStarted;
    private String statusStopped;
    private String health;


    public ServiceRoleInfo (String fullName, RoleType role) {
        this.fullName = fullName;
        this.roleName = role.toString();
    }
            
//    public ServiceRoleInfo (String name, String shortName, String statusStarted, String statusStopped, String health) {
//        this.fullName = name;
//        this.roleName = shortName;
//        this.statusStarted = statusStarted;
//        this.statusStopped = statusStopped;
//        this.health = health;
//                
//    }
//    
    public String getFullName() {
        return fullName;
    }
    
    public String getRoleName(){
        return roleName;
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