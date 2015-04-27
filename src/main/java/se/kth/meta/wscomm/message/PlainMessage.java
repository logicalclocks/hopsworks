
package se.kth.meta.wscomm.message;

import se.kth.meta.entity.EntityIntf;
import java.util.LinkedList;
import java.util.List;
import javax.json.JsonObject;

/**
 *
 * @author Vangelis
 */
public abstract class PlainMessage implements Message {

    @Override
    public void setAction(String action){
        
    }
    
    @Override
    public String getAction() {
        return null;
    }

    @Override
    public void setStatus(String status){}

    @Override
    public String getStatus(){
        return "No status";
    }

    @Override
    public void init(JsonObject obj) {
    }

    @Override
    public String encode() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<EntityIntf> parseSchema() {
        return new LinkedList<>();
    }
    
    @Override
    public String buildSchema(List<EntityIntf> tables){
        return "PlainMessage.java";
    }

    @Override
    public abstract String getMessage();

    @Override
    public abstract void setMessage(String msg);

    @Override
    public abstract String getSender();

    @Override
    public abstract void setSender(String sender);

}
