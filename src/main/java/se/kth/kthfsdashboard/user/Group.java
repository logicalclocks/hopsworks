/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
public enum Group {
    
    ADMIN("admin"), 
    USER("user"), 
    AGENT("agent"), 
    BBC_RESEARCHER("bbc researcher"), 
    BBC_ADMIN("bbc admin");
    
    private final String group;

    private Group(String group) {
        this.group = group;
    }

    public String getLabel() {
        return group;
    }
       
}
