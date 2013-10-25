/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.virtualization.clusterparser;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public class GlobalProperties {
    private String environment;
    private List<String> recipes= new ArrayList<String>();
    private List<Integer> authorizePorts = new ArrayList<Integer>();
    private GitProperties git;

    public GlobalProperties() {
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public List<String> getRecipes() {
        return recipes;
    }

    public void setRecipes(List<String> recipes) {
        this.recipes = recipes;
    }

    public List<Integer> getAuthorizePorts() {
        return authorizePorts;
    }

    public void setAuthorizePorts(List<Integer> authorizePorts) {
        this.authorizePorts = authorizePorts;
    }

    public GitProperties getGit() {
        return git;
    }

    public void setGit(GitProperties git) {
        this.git = git;
    }
    
    
}
