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
import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

@ManagedBean
@SessionScoped
public class BBCGroups implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<String, Integer> groups = new HashMap<>();
    private String group;

    public BBCGroups(){
        groups = new HashMap<>();
        groups.put("BBC_ADMIN", 1001);
        groups.put("BBC_RESEARCHER", 1002);
        groups.put("BBC_GUEST", 1003);
        groups.put("AUDITOR", 1004);
     //   groups.put("ETHICS_BOARD", 1005);
        groups.put("SYS_ADMIN", 1006);
        groups.put("BBC_USER", 1007);
    }


    @PostConstruct
    public void init() {
        // group names
        groups = new HashMap<>();
        groups.put("BBC_ADMIN", 1001);
        groups.put("BBC_RESEARCHER", 1002);
        groups.put("BBC_GUEST", 1003);
        groups.put("AUDITOR", 1004);
    //    groups.put("ETHICS_BOARD", 1005);
        groups.put("SYS_ADMIN", 1006);
        groups.put("BBC_USER", 1007);
    }

   public Integer getGroupNum( String value) {
    
       for (Entry<String, Integer> entrySet : groups.entrySet()) {
           String key = entrySet.getKey();
           Integer value1 = entrySet.getValue();
           
           if(key.equals(value))
               return value1;
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