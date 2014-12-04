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
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;

@ManagedBean
@RequestScoped
public class SelectSecurityQuestionMenue {
    
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    private String question;
    private Map<String, String> questions = new HashMap<>();

    @EJB
    private UserManager mgr;
     
    @PostConstruct
    public void init() {
        // security questions
        questions = new HashMap<>();
        questions.put("Who is your favorite historical figure?", "history");
        questions.put("What is name of your favorite teacher?", "teacher");
        questions.put("What is your first phone number?", "phone");
        questions.put("What is the name of your favorite childhood friend?", "phone");

    }
    
    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public Map<String, String> getQuestions() {
        return questions;
    }
    
}