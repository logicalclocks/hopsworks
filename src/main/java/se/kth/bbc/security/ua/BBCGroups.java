/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;


import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean
@SessionScoped
public class BBCGroups implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = Logger.getLogger(UserRegistration.class.getName());

    public BBCGroups(){
        groups = new HashMap<>();
        groups.put("BBC_ADMIN", 1001);
        groups.put("BBC_RESEARCHER", 1002);
        groups.put("BBC_GUEST", 1003);
        groups.put("AUDITOR", 1004);
        groups.put("ETHICS_BOARD", 1005);

    }

    private String group;
    private Map<String, Integer> groups = new HashMap<>();

    @PostConstruct
    public void init() {
        // group names
        groups = new HashMap<>();
        groups.put("BBC_ADMIN", 1001);
        groups.put("BBC_RESEARCHER", 1002);
        groups.put("BBC_GUEST", 1003);
        groups.put("AUDITOR", 1004);
        groups.put("ETHICS_BOARD", 1005);

    }

   public Integer getGroupNum( String value) {
        for (Object o : groups.keySet()) {
           if (groups.get(o).equals(value)) {
                return (Integer)o;
            }
        }
        return -1;
    }
    
   public String getGroupName(int gid) {
       
         for (Entry<String, Integer> entry : groups.entrySet()) {
            if (entry.getValue().equals(gid)) {
               return entry.getKey();
            }
        }
         return "";
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public Map<String, Integer> getGroups() {
        return groups;
    }

    public void setGroups(Map<String, Integer> groups) {
        this.groups = groups;
    }
}    
    
