package io.hops.hopsworks.api.certs;

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
}
