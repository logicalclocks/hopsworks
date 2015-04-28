package se.kth.hopsworks.rest;

import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@XmlRootElement //we don't need this thanks to Jackson
//@JsonSerialize(include = JsonSerialize.Inclusion.ALWAYS)  
//@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)  
public class JsonResponse{
     
    private static final float version = 1.0f;  
 
    private String status;
    private Integer statusCode;
    private String errorMsg;
    private String successMessage;
    private List<String> fieldErrors;
    private Object data;
    private String sessionID;
 
    public JsonResponse() {
    }
     
    public JsonResponse(String status) {
        this.status = status;
    }  

    public JsonResponse(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }
    
    
     
    @XmlElement
    public float getVersion() {
        return JsonResponse.version;
    }
         
    public String getStatus() {
        return status;
    }
 
    public void setStatus(String status) {
        this.status = status;
    }
    
    @XmlElement
    public String getErrorMsg() {
        return errorMsg;
    }
 
    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public List<String> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<String> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
    
 
   
    
    @XmlElement
    public Object getData() {
        return data;
    }
 
    public void setData(Object data) {
        this.data = data;
    }
    
    @XmlElement
    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }
     
}