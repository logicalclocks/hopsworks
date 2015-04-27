
package se.kth.meta.wscomm.message;

/**
 * Represents the command each incoming message is transformed to
 * 
 * @author vangelis
 */
public enum Command {
    
    STORE_FIELD,
    //
    STORE_TEMPLATE,
    //
    STORE_METADATA,
    //
    ADD_NEW_TEMPLATE,
    //
    REMOVE_TEMPLATE,
    //
    FETCH_TEMPLATE,
    //
    FETCH_TEMPLATES,
    //
    FETCH_METADATA,
    //
    DELETE_TABLE,
    //
    DELETE_FIELD,
    //
    BROADCAST,
    //
    QUIT,
    //
    TEST
}
