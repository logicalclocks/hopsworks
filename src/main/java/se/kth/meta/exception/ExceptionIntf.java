
package se.kth.meta.exception;

/**
 *
 * @author Vangelis
 */
public abstract class ExceptionIntf extends Exception {

    public ExceptionIntf(String message){
        super(message);
    }
    
    
    @Override
    public abstract String getMessage();
}
