package io.hops.hopsworks.common.exception;

import javax.ws.rs.core.Response;

public class DeviceServiceException extends Exception {

  private Response response;

  public DeviceServiceException(Response response){
    this.response = response;
  }

  public Response getResponse(){
    return this.response;
  }
}
