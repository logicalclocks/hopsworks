package io.hops.hopsworks.api.filter;

import io.hops.hopsworks.api.util.JsonResponse;

import javax.ejb.Stateless;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

@Stateless
public class NoCacheResponse {

  public ResponseBuilder getNoCacheResponseBuilder(Response.Status status) {
    CacheControl cc = new CacheControl();
    cc.setNoCache(true);
    cc.setMaxAge(-1);
    cc.setMustRevalidate(true);

    return Response.status(status).cacheControl(cc);
  }

  public ResponseBuilder getNoCacheCORSResponseBuilder(Response.Status status) {
    CacheControl cc = new CacheControl();
    cc.setNoCache(true);
    cc.setMaxAge(-1);
    cc.setMustRevalidate(true);
    return Response.status(status)
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET")
            .cacheControl(cc);
  }
  
  public JsonResponse buildJsonResponse(Response.Status status, String message) {
    JsonResponse response = new JsonResponse();
    response.setStatus(String.valueOf(status));
    response.setSuccessMessage(message);
    
    return response;
  }
}
