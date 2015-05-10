/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.controller;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.study.StudyTeam;

/**
 *
 * @author Ermias
 */
@XmlRootElement
public class MembersDTO {
   private List<StudyTeam> projectTeam;

  public MembersDTO() {
  }

  public MembersDTO(List<StudyTeam> projectTeam) {
    this.projectTeam = projectTeam;
  }

  public List<StudyTeam> getProjectTeam() {
    return projectTeam;
  }

  public void setProjectTeam(List<StudyTeam> projectTeam) {
    this.projectTeam = projectTeam;
  }

  @Override
  public String toString() {
    return "MembersDTO{" + "projectTeam=" + projectTeam + '}';
  }
   
}
