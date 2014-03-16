/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hop.deploy.virtualization.parser;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class GitProperties {
    private String user;
    private String repository;
    private String key;

    public GitProperties() {
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
    
}
