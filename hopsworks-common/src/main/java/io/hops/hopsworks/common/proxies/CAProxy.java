/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.common.proxies;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.base.Strings;
import io.hops.hopsworks.common.proxies.client.HttpClient;
import io.hops.hopsworks.common.proxies.client.HttpRetryableAction;
import io.hops.hopsworks.common.proxies.client.NotRetryableClientProtocolException;
import io.hops.hopsworks.common.security.CSR;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.exceptions.CAException;
import io.hops.hopsworks.exceptions.GenericException;
import io.hops.hopsworks.exceptions.HopsSecurityException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpStatus;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class CAProxy {
  private final static Logger LOG = Logger.getLogger(CAProxy.class.getName());
  
  private final static String CONTENT_TYPE_JSON = "application/json; charset=utf-8";
  private final static String CA_BASE_PATH = "/hopsworks-ca/v2/certificate/";
  private final static String CERTIFICATE_IDENTIFIER = "certId";
  
  private ObjectMapper objectMapper;
  private ResponseHandler<CSR> CA_SIGN_RESPONSE_HANDLER = new CASignCSRResponseHandler();
  private ResponseHandler<Void> CA_REVOKE_RESPONSE_HANDLER = new CARevokeX509ResponseHandler();
  
  private enum CA_PATH {
    PROJECT_CA_PATH(CA_BASE_PATH + "project"),
    DELA_CA_PATH(CA_BASE_PATH + "dela");
    
    private final String path;
    
    CA_PATH(String path) {
      this.path = path;
    }
    
  }
  
  @EJB
  private HttpClient client;
  @EJB
  private Settings settings;
  
  @PostConstruct
  public void init() {
    objectMapper = new ObjectMapper();
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
    SimpleModule module = new SimpleModule();
    module.addDeserializer(CAException.class, new CAExceptionDeserializer());
    objectMapper.registerModule(module);
  }
  
  public CSR signProjectCSR(CSR csr) throws HopsSecurityException, GenericException {
    return signCSR(csr, CA_PATH.PROJECT_CA_PATH);
  }
  
  public CSR signDelaCSR(CSR csr) throws HopsSecurityException, GenericException {
    return signCSR(csr, CA_PATH.DELA_CA_PATH);
  }
  
  private CSR signCSR(CSR csr, CA_PATH path) throws HopsSecurityException, GenericException{
    try {
      String csrJSON = objectMapper.writeValueAsString(csr);
      HttpPost httpRequest = new HttpPost(path.path);
      httpRequest.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE_JSON);
      client.setAuthorizationHeader(httpRequest);
      httpRequest.setEntity(new StringEntity(csrJSON));
    
      HttpRetryableAction<CSR> retryableAction = new HttpRetryableAction<CSR>() {
        @Override
        public CSR performAction() throws ClientProtocolException, IOException {
          return client.execute(httpRequest, CA_SIGN_RESPONSE_HANDLER);
        }
      };
      return retryableAction.tryAction();
    } catch (JsonProcessingException ex) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CSR_ERROR, Level.SEVERE, null, null, ex);
    } catch (ClientProtocolException ex) {
      LOG.log(Level.SEVERE, "Could not sign CSR", ex);
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CSR_ERROR, Level.SEVERE, null, null, ex.getCause());
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Could not sign CSR", ex);
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE,
          "Generic error while signing CSR", null, ex);
    }
  }
  
  public void revokeProjectX509(String certificateIdentifier) throws HopsSecurityException, GenericException {
    revokeX509(certificateIdentifier, CA_PATH.PROJECT_CA_PATH);
  }
  
  public void revokeDelaX509(String certificateIdentifier) throws HopsSecurityException, GenericException {
    revokeX509(certificateIdentifier, CA_PATH.DELA_CA_PATH);
  }
  
  private void revokeX509(String certificateIdentifier, CA_PATH path) throws HopsSecurityException, GenericException {
    if (Strings.isNullOrEmpty(certificateIdentifier)) {
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERTIFICATE_NOT_FOUND, Level.SEVERE,
          null, "Certificate Identifier cannot be null or empty");
    }
    try {
      URI revokeURI = new URIBuilder(path.path)
          .addParameter(CERTIFICATE_IDENTIFIER, certificateIdentifier)
          .build();
      HttpDelete httpRequest = new HttpDelete(revokeURI);
      client.setAuthorizationHeader(httpRequest);
      
      HttpRetryableAction<Void> retryableAction = new HttpRetryableAction<Void>() {
        @Override
        public Void performAction() throws ClientProtocolException, IOException {
          return client.execute(httpRequest, CA_REVOKE_RESPONSE_HANDLER);
        }
      };
      retryableAction.tryAction();
    } catch (URISyntaxException ex) {
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE, null, null, ex);
    } catch (ClientProtocolException ex) {
      LOG.log(Level.WARNING, "Could not revoke X.509 " + certificateIdentifier, ex);
      if (ex.getCause() instanceof HopsSecurityException) {
        throw (HopsSecurityException) ex.getCause();
      }
      throw new HopsSecurityException(RESTCodes.SecurityErrorCode.CERTIFICATE_REVOKATION_ERROR, Level.WARNING,
          null, null, ex);
    } catch (IOException ex) {
      LOG.log(Level.SEVERE, "Could not revoke X.509 " + certificateIdentifier, ex);
      throw new GenericException(RESTCodes.GenericErrorCode.UNKNOWN_ERROR, Level.SEVERE,
          "Generic error while revoking X.509", null, ex);
    }
  }
  
  private class CASignCSRResponseHandler implements ResponseHandler<CSR> {
  
    @Override
    public CSR handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      if (status / 100 == 4) {
        throw new NotRetryableClientProtocolException(constructSignHopsSecurityException(response));
      } else if (status / 100 != 2) {
        throw new ClientProtocolException(constructSignHopsSecurityException(response));
      }
      String responseJSON = EntityUtils.toString(response.getEntity(), Charset.defaultCharset());
      return objectMapper.readValue(responseJSON, CSR.class);
    }
    
    private HopsSecurityException constructSignHopsSecurityException(HttpResponse response) throws IOException {
      return constructHopsSecurityException(response, RESTCodes.SecurityErrorCode.CERTIFICATE_SIGN_USER_ERR,
          Level.FINE);
    }
  }
  
  private HopsSecurityException constructHopsSecurityException(HttpResponse response,
      RESTCodes.SecurityErrorCode errorCode, Level logLevel)
    throws IOException {
    CAException exception = objectMapper.readValue(response.getEntity().getContent(), CAException.class);
    return new HopsSecurityException(errorCode, logLevel, exception.getUsrMsg(), exception.getDevMsg());
  }
  
  private class CARevokeX509ResponseHandler implements ResponseHandler<Void> {
  
    @Override
    public Void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
      int status = response.getStatusLine().getStatusCode();
      if (HttpStatus.SC_OK == status) {
        return null;
      }
      if (HttpStatus.SC_NO_CONTENT == status) {
        // The revoke endpoint returns NO_CONTENT if it cannot find the certificate.
        HopsSecurityException securityException = new HopsSecurityException(
            RESTCodes.SecurityErrorCode.CERTIFICATE_NOT_FOUND, Level.WARNING);
        throw new NotRetryableClientProtocolException(securityException);
      }
      if (HttpStatus.SC_BAD_REQUEST == status) {
        HopsSecurityException securityException = constructHopsSecurityException(response,
            RESTCodes.SecurityErrorCode.CERTIFICATE_REVOKATION_USER_ERR, Level.FINE);
        throw new NotRetryableClientProtocolException(securityException);
      }
      throw new ClientProtocolException("Temporary error while revoking certificate, HTTP status: " + status);
    }
  }
}
