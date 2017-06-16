/*
 * Copyright 2012 predic8 GmbH, www.predic8.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.predic8.membrane.servlet.embedded;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.http.PlainBodyTransferrer;
import com.predic8.membrane.core.http.Header;
import com.predic8.membrane.core.http.HeaderField;
import com.predic8.membrane.core.http.Request;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.transport.Transport;
import com.predic8.membrane.core.transport.http.AbortException;
import com.predic8.membrane.core.transport.http.AbstractHttpHandler;
import com.predic8.membrane.core.transport.http.EOFWhileReadingFirstLineException;
import com.predic8.membrane.core.util.EndOfStreamException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpHost;
import org.apache.http.client.utils.URIUtils;

class HopsServletHandler extends AbstractHttpHandler {

  private static final Log log = LogFactory.getLog(HopsServletHandler.class);

  private final HttpServletRequest request;
  private final HttpServletResponse response;
//  private URI srcUriObj = null;
  private URI targetUriObj = null;
  private HttpHost targetHost = null;

  public HopsServletHandler(HttpServletRequest request,
          HttpServletResponse response,
          Transport transport, URI targetUriObj) throws IOException {
    super(transport);
    this.request = request;
    this.response = response;
//    this.srcUriObj = srcUri;
    this.targetUriObj = targetUriObj;
    this.targetHost = URIUtils.extractHost(targetUriObj);

    exchange = new Exchange(this);

    exchange.setProperty(Exchange.HTTP_SERVLET_REQUEST, request);
  }

  public void run() {
    try {
      srcReq = createRequest();

      exchange.received();

      try {
//        DNSCache dnsCache = getTransport().getRouter().getDnsCache();
//        String ip = dnsCache.getHostAddress(remoteAddr);
        exchange.setRemoteAddrIp("127.0.0.1");
        exchange.setRemoteAddr(this.targetHost.getHostName());
//        exchange.setRemoteAddr(this.targetUriObj.toString());
//        exchange.setRemoteAddr(getTransport().isReverseDNS() ? dnsCache.
//                getHostName(remoteAddr) : ip);

        exchange.setRequest(srcReq);
        exchange.setOriginalRequestUri(srcReq.getUri());

        invokeHandlers();
      } catch (AbortException e) {
        exchange.finishExchange(true, exchange.getErrorMessage());
        writeResponse(exchange.getResponse());
        return;
      }

      exchange.getRequest().readBody(); // read if not alread read
      writeResponse(exchange.getResponse());
      exchange.setCompleted();
    } catch (EndOfStreamException e) {
      log.debug("stream closed");
    } catch (EOFWhileReadingFirstLineException e) {
      log.debug(
              "Client connection terminated before line was read. Line so far: ("
              + e.getLineSoFar() + ")");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    } finally {
      exchange.detach();
    }

  }

  @SuppressWarnings("deprecation")
  protected void writeResponse(Response res) throws Exception {
    response.setStatus(res.getStatusCode(), res.getStatusMessage());
    for (HeaderField header : res.getHeader().getAllHeaderFields()) {
      if (header.getHeaderName().equals(Header.TRANSFER_ENCODING)) {
        continue;
      }
      response.addHeader(header.getHeaderName().toString(), header.getValue());
    }

    ServletOutputStream out = response.getOutputStream();
    res.getBody().write(new PlainBodyTransferrer(out));
    out.flush();

    response.flushBuffer();

    exchange.setTimeResSent(System.currentTimeMillis());
    exchange.collectStatistics();
  }

  private Request createRequest() throws IOException {
    Request srcReq = new Request();

    String pathQuery = this.targetHost.toURI();
    pathQuery += request.getRequestURI();
    if (request.getQueryString() != null) {
      pathQuery += "?" + request.getQueryString();
    }
//    String pathQuery = this.targetUriObj.toString();

    if (getTransport().isRemoveContextRoot()) {
      String contextPath = request.getContextPath();
      if (contextPath.length() > 0 && pathQuery.startsWith(contextPath)) {
        pathQuery = pathQuery.substring(contextPath.length());
      }
    }

    srcReq.create(
            request.getMethod(),
            pathQuery,
            request.getProtocol(),
            createHeader(),
            request.getInputStream());
    return srcReq;
  }

  private Header createHeader() {
    Header header = new Header();
    Enumeration<?> e = request.getHeaderNames();
    while (e.hasMoreElements()) {
      String key = (String) e.nextElement();
      Enumeration<?> e2 = request.getHeaders(key);
      while (e2.hasMoreElements()) {
        String value = (String) e2.nextElement();
        header.add(key, value);
      }
    }
    try {
      header.add(Header.DESTINATION, targetUriObj.toURL().toString());
    } catch (MalformedURLException ex) {
      Logger.getLogger(HopsServletHandler.class.getName()).
              log(Level.SEVERE, null, ex);
    }
    return header;
  }

  @Override
  public void shutdownInput() throws IOException {
    request.getInputStream().close();
    // nothing more we can do, since the servlet API does not give
    // us access to the TCP API
  }

  @Override
  public InetAddress getLocalAddress() {
    try {
      //    return localAddr;
      return InetAddress.getLocalHost();
    } catch (UnknownHostException ex) {
      Logger.getLogger(HopsServletHandler.class.getName()).
              log(Level.SEVERE, null, ex);
    }
    return null;
  }

  @Override
  public int getLocalPort() {
    return request.getLocalPort();
//    return -1;
  }

  @Override
  public HopsTransport getTransport() {
    return (HopsTransport) super.getTransport();
  }

  @Override
  public boolean isMatchLocalPort() {
    return false;
  }

  @Override
  public String getContextPath(Exchange exc) {
    return ((HttpServletRequest) exc.getProperty(Exchange.HTTP_SERVLET_REQUEST)).
            getContextPath();
  }

  @Override
  public OutputStream getSrcOut() {
    try {
      return response.getOutputStream();
    } catch (IOException ex) {
      Logger.getLogger(HopsServletHandler.class.getName()).
              log(Level.SEVERE, null, ex);
    }
    return null;
  }

  @Override
  public InputStream getSrcIn() {
    try {
      return request.getInputStream();
    } catch (IOException ex) {
      Logger.getLogger(HopsServletHandler.class.getName()).
              log(Level.SEVERE, null, ex);
    }
    return null;
  }

  @Override
  public String getRemoteAddress() {
    return request.getRemoteAddr();
  }

}
