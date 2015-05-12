
package se.kth.meta.exception;

/**
 *
 * @author Vangelis
 */
public class DatabaseException extends ExceptionIntf {

    private String message;
    private String Class;

    public DatabaseException(String message) {
        super(message);
        this.message = message;
    }
    
    public DatabaseException(String Class, String message){
        this(message);
        this.Class = Class;
    }
     

    @Override
    public String getMessage() {
        return this.message;
    }
}
