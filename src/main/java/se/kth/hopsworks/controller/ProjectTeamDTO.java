/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

import java.util.Date;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.study.StudyRoleTypes;

/**
 *
 * @author Ermias
 */
@XmlRootElement
public class ProjectTeamDTO {
    private String projectName;
    private String userName;
    private StudyRoleTypes role;
    private Date added;

    public ProjectTeamDTO() {
    }

    public ProjectTeamDTO(String userName, StudyRoleTypes role) {
        this.userName = userName;
        this.role = role;
    }

    public ProjectTeamDTO(String projectName, String userName, StudyRoleTypes role) {
        this.projectName = projectName;
        this.userName = userName;
        this.role = role;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public StudyRoleTypes getRole() {
        return role;
    }

    public void setRole(StudyRoleTypes role) {
        this.role = role;
    }

    public Date getAdded() {
        return added;
    }

    public void setAdded(Date added) {
        this.added = added;
    }
    
    
}
