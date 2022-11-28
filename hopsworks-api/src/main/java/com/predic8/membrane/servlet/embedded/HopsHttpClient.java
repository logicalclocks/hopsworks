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

package com.predic8.membrane.servlet.embedded;

import com.predic8.membrane.core.exchange.Exchange;
import com.predic8.membrane.core.model.AbstractExchangeViewerListener;
import com.predic8.membrane.core.transport.http.AbstractHttpHandler;
import com.predic8.membrane.core.transport.http.Connection;
import com.predic8.membrane.core.transport.http.HttpClient;
import com.predic8.membrane.core.transport.http.StreamPump;
import com.predic8.membrane.core.transport.http.WebSocketStreamPump;
import com.predic8.membrane.core.transport.http.client.HttpClientConfiguration;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.RejectedExecutionException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class HopsHttpClient extends HttpClient {

  private static final Logger LOGGER = Logger.getLogger(HopsHttpClient.class.getName());

  private ManagedExecutorService managedExecutorService;

  public HopsHttpClient(HttpClientConfiguration configuration,ManagedExecutorService managedExecutorService ) {
    super(configuration);

    this.managedExecutorService = managedExecutorService;
  }

  @Override
  public void setupConnectionForwarding(Exchange exc, final Connection con, final String protocol,
                                        StreamPump.StreamPumpStats streamPumpStats) throws SocketException {
    final AbstractHttpHandler hsr = exc.getHandler();
    String source = hsr.getRemoteAddress();
    String dest = con.toString();
    final StreamPump a;
    final StreamPump b;
    if("WebSocket".equals(protocol)){
      WebSocketStreamPump aTemp = new WebSocketStreamPump(hsr.getSrcIn(), con.out, streamPumpStats,
          protocol + " " + source + " -> " + dest, exc.getRule(),true,exc);
      WebSocketStreamPump bTemp = new WebSocketStreamPump(con.in, hsr.getSrcOut(), streamPumpStats,
          protocol + " " + source + " <- " + dest, exc.getRule(),false, null);
      aTemp.init(bTemp);
      bTemp.init(aTemp);
      a = aTemp;
      b = bTemp;
    } else {
      a = new StreamPump(hsr.getSrcIn(), con.out, streamPumpStats,
          protocol + " " + source + " -> " + dest, exc.getRule());
      b = new StreamPump(con.in, hsr.getSrcOut(), streamPumpStats,
          protocol + " " + source + " <- " + dest, exc.getRule());
    }

    exc.addExchangeViewerListener(new AbstractExchangeViewerListener() {
      @Override
      public void setExchangeFinished() {
        // Backward Thread
        try {
          if (managedExecutorService != null) {
            managedExecutorService.submit(b);
          }
          // Onward Thread
          a.run();
         
        } catch (RejectedExecutionException e) {
          LOGGER.log(Level.WARNING, "Too many notebooks opened - jupyter executor pool is full", e);
          throw e;
        } finally {
          try {
            con.close();
          } catch (IOException e) {
            LOGGER.log(Level.FINE, "", e);
          }
        }
      }
    });
  }
}
