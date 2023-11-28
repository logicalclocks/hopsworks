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
import javax.naming.InitialContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.logging.Logger;
import java.util.logging.Level;

public class HopsHttpClient extends HttpClient {
  private static final Logger LOGGER = Logger.getLogger(HopsHttpClient.class.getName());
  private ManagedExecutorService managedExecutorService;

  public HopsHttpClient(HttpClientConfiguration configuration) {
    super(configuration);
    try {
      managedExecutorService = InitialContext.doLookup("concurrent/hopsExecutorService");
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, "Error looking up for the hopsExecutorService", e);
    }
  }

  @Override
  public void setupConnectionForwarding(Exchange exchange, final Connection connection, final String protocol,
                                        StreamPump.StreamPumpStats streamPumpStats) throws SocketException {
    final AbstractHttpHandler httpHandler = exchange.getHandler();
    String sourceAddress = httpHandler.getRemoteAddress();
    String destinationAddress = connection.toString();

    final StreamPump streamPumpForward;
    final StreamPump streamPumpBackward;

    if ("WebSocket".equals(protocol)) {
      streamPumpForward = createWebSocketStreamPump(httpHandler.getSrcIn(), connection.out, streamPumpStats,
              protocol, sourceAddress, destinationAddress, exchange, true);
      streamPumpBackward = createWebSocketStreamPump(connection.in, httpHandler.getSrcOut(), streamPumpStats,
              protocol, sourceAddress, destinationAddress, exchange, false);
      initializeStreamPumps(streamPumpForward, streamPumpBackward);
    } else {
      streamPumpForward = createStreamPump(httpHandler.getSrcIn(), connection.out, streamPumpStats,
              protocol, sourceAddress, destinationAddress, exchange);
      streamPumpBackward = createStreamPump(connection.in, httpHandler.getSrcOut(), streamPumpStats,
              protocol, sourceAddress, destinationAddress, exchange);
    }

    exchange.addExchangeViewerListener(new AbstractExchangeViewerListener() {
      @Override
      public void setExchangeFinished() {
        executeStreamPump(streamPumpBackward);
        executeStreamPump(streamPumpForward, connection);
      }
    });
  }

  private StreamPump createStreamPump(InputStream srcIn, OutputStream srcOut, StreamPump.StreamPumpStats stats,
                                      String protocol, String source, String destination, Exchange exchange) {
    return new StreamPump(srcIn, srcOut, stats, protocol + " " + source + " -> " + destination, exchange.getRule());
  }

  private WebSocketStreamPump createWebSocketStreamPump(InputStream srcIn, OutputStream srcOut,
                                                        StreamPump.StreamPumpStats stats, String protocol,
                                                        String source, String destination, Exchange exchange,
                                                        boolean isForwardDirection) {
    return new WebSocketStreamPump(srcIn, srcOut, stats, protocol + " " + source + (isForwardDirection ? " -> " : " <- ") + destination,
            exchange.getRule(), isForwardDirection, isForwardDirection ? exchange : null);
  }

  private void initializeStreamPumps(StreamPump pumpA, StreamPump pumpB) {
    ((WebSocketStreamPump) pumpA).init((WebSocketStreamPump) pumpB);
    ((WebSocketStreamPump) pumpB).init((WebSocketStreamPump) pumpA);
  }

  private void executeStreamPump(StreamPump streamPump) {
    if (managedExecutorService != null) {
      managedExecutorService.submit(streamPump);
    }
  }

  private void executeStreamPump(StreamPump streamPump, Connection connection) {
    try {
      streamPump.run();
    } finally {
      closeConnection(connection);
    }
  }

  private void closeConnection(Connection connection) {
    try {
      connection.close();
    } catch (IOException e) {
      LOGGER.log(Level.FINE, "", e);
    }
  }
}
