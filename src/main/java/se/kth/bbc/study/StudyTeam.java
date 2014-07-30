/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.study;



/**
 *
 * @author roshan
 */
public enum StudyTeam {
    
  MASTER("Master"),
  RESEARCHER("Researcher"),
  GUEST("Guest");
  
  String role;
   
  StudyTeam(String role){
      this.role = role;
  }
  
//  @Override
//  public String toString(){
//      return role;
//  }
  
  public String getTeam() {
        return role;
  }
       
}
