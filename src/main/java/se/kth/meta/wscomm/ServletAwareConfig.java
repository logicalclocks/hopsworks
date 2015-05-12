
package se.kth.meta.wscomm;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

/**
 *
 * @author Vangelis
 */
public class ServletAwareConfig extends ServerEndpointConfig.Configurator {

    /**
     * Intercept the handshake operation so that we can take a hold of the 
     * ServletContext instance to be able to retrieve attributes stored to it
     * such as the database object and other similar class instances
     * 
     * @param config
     * @param request
     * @param response
     */
    @Override
    public void modifyHandshake(ServerEndpointConfig config, 
            HandshakeRequest request, HandshakeResponse response) {
        
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        System.out.println("SESSION IS NULL " + (httpSession == null));
        System.out.println("WSSESSIONID " + httpSession.getId());
        ServletContext context = (ServletContext) httpSession.getServletContext();
        
        config.getUserProperties().put("httpSession", httpSession);
        
        /**
         * store these attributes to servletContext so that they are available to 
         * every created user socket session
         */
        config.getUserProperties().put("db", context.getAttribute("db"));
        config.getUserProperties().put("protocol", context.getAttribute("protocol"));
    }
}
