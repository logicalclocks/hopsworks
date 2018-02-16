/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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
