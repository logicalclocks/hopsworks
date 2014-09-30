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

   //ADMIN, USER, AGENT, BBC_RESEARCHER, BBC_ADMIN, GUEST;
    USER("User"),
    ADMIN("Admin"),
    AGENT("Agent"),
    BBC_RESEARCHER("BBC Researcher"),
    BBC_ADMIN("BBC Admin"),
    GUEST("GUEST"); // A newly registered user is but a guest: has no access to anything.

    private final String group;

    private Group(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

}
