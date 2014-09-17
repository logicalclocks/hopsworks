/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study;





/**
 *
 * @author roshan
 */
public enum StudyRoleTypes {
    
  MASTER("Master"),
  RESEARCHER("Researcher"),
  AUDITOR("Auditor");
  
  String role;
   
  StudyRoleTypes(String role){
      this.role = role;
  }
  
  public String getTeam() {
      return this.role;
  }
       
}
