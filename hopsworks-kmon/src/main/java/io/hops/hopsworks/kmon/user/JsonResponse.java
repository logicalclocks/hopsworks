package io.hops.hopsworks.kmon.user;

import java.util.Map;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement //we don't need this thanks to Jackson
//@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)  
public class JsonResponse {

  private static final float version = 1.0f;

  private String status;
  private String errorMsg;
  private Map<String, Object> fieldErrors;
  private Object data;

  public JsonResponse() {
  }

  public JsonResponse(String status) {
    this.status = status;
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

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public Map<String, Object> getFieldErrors() {
    return fieldErrors;
  }

  public void setFieldErrors(Map<String, Object> fieldErrors) {
    this.fieldErrors = fieldErrors;
  }

  public Object getData() {
    return data;
  }

  public void setData(Object data) {
    this.data = data;
  }

}
