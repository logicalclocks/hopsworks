package com.predic8.membrane.servlet.embedded;



import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.http.HttpServletRequest;
import com.predic8.membrane.core.http.Header;
import com.predic8.membrane.core.http.Request;
import org.apache.http.HttpHost;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RequestCreator {
    private HttpServletRequest request;
    private HttpHost targetHost;

    private static Logger LOGGER = Logger.getLogger(RequestCreator.class.getName());

    public RequestCreator(HttpServletRequest request, HttpHost targetHost) {
        this.request = request;
        this.targetHost = targetHost;
    }

    public Request createRequest() throws IOException {
        Request srcReq = new Request();
        String pathQuery = createPathQuery();
        srcReq.create(request.getMethod(), pathQuery, request.getProtocol(), createHeader(), request.getInputStream());
        return srcReq;
    }

    private String createPathQuery() {
        String pathQuery = targetHost.toURI();
        pathQuery += request.getRequestURI();
        if (request.getQueryString() != null) {
            pathQuery += "?" + request.getQueryString();
        }
        return pathQuery;
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
            header.add(Header.DESTINATION, targetHost.toURI().toString());
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return header;
    }
}

