package com.predic8.membrane.servlet.embedded;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import com.predic8.membrane.core.http.Header;
import com.predic8.membrane.core.http.Response;
import com.predic8.membrane.core.http.HeaderField;
import com.predic8.membrane.core.http.PlainBodyTransferrer;

public class ResponseWriter {

    private HttpServletResponse response;

    public ResponseWriter(HttpServletResponse response) {
        this.response = response;
    }

    public void writeResponse(Response res) throws Exception {
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
    }
}
