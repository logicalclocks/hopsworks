package se.kth.bbc.security.auth;

/**
 * This class contains the error messages that user get upon wrong authentication.
 * 
 * @author Ali Gholami <gholami@pdc.kth.se>
 */
public class AccountStatusErrorMessages {

    public final static String INCCORCT_CREDENTIALS ="The email or password is incorrect.";
    
    public final static String USER_NOT_FOUND ="No account found.";
    
    public final static String BLOCKED_ACCOUNT ="This account is blocked.";
    
    public final static String INACTIVE_ACCOUNT ="This account is not activated.";
    
    public final static String INVALID_SEQ_ANSWER ="Inccorrect answer.";
    
    public final static String DEACTIVATED_ACCOUNT ="This account is deactivated.";
    
    public final static String INCORRECT_PIN ="PIN must be 6 charachters.";
    
    public final static String PIN_REQUIERMENTS ="PIN must be numeric.";
    
    public final static String PASSWORD_REQUIREMNTS="Password must contain at least 6 characters.";
    
}
