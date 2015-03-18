/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.bbc.security.ua;


/**
 * 
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
public enum SecurityQuestions {
    
    HISTORY("Who is your favorite historical figure?"),
    TEACHER("What is the name of your favorite teacher?"),
    PHONE("What is your first phone number?"),
    FRIEND("What is the name of your favorite childhood friend?");

    private final String value;

    private SecurityQuestions(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
    
    public static SecurityQuestions getQuestion(String text) {
    if (text != null) {
      for (SecurityQuestions b : SecurityQuestions.values()) {
        if (text.equalsIgnoreCase(b.value)) {
          return b;
        }
      }
    }
    return null;
  }
}
