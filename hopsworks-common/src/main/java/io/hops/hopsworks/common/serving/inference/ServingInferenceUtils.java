/*
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
 */

package io.hops.hopsworks.common.serving.inference;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class ServingInferenceUtils {
  
  public HttpPost buildInferenceRequest(String host, int port, String path, String json) throws URISyntaxException {
    URI uri = new URIBuilder()
      .setScheme("http")
      .setHost(host)
      .setPort(port)
      .setPath(path)
      .build();
    
    ContentType contentType = ContentType.create(ContentType.APPLICATION_JSON.getMimeType(), StandardCharsets.UTF_8);
    StringEntity payload = new StringEntity(json, contentType);
    
    HttpPost request = new HttpPost(uri);
    request.addHeader("content-type", contentType.toString());
    request.setEntity(payload);
    
    return request;
  }
}
