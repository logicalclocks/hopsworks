package se.kth.meta.wscomm;

import se.kth.meta.db.Dbao;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.EncodeException;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import se.kth.meta.listener.ApplicationListener;
import se.kth.meta.wscomm.message.Message;
import se.kth.meta.wscomm.message.TextMessage;

/**
 *
 * @author Vangelis
 */
@ServerEndpoint(value = "/wspoint/{*}",
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class,
        configurator = ServletAwareConfig.class)
public class WebSocketEndpoint {

    private static final Logger logger = Logger.getLogger(WebSocketEndpoint.class.getName());

    private Set<Session> sessions = new HashSet<>();
    private Session mySession;
    private String sender;
    private Dbao db;
    private Protocol protocol;

    public Set<Session> getOpenSessions() {
        return sessions;
    }

    @OnOpen
    public void open(Session session, EndpointConfig config,
            @PathParam("*") String sender) {

        this.sender = sender;
        this.mySession = session;

        logger.log(Level.SEVERE, "CONNECTED USER {0}", sender);

        //keep track of each new client session
        this.addSession(session);
        //this.db = (Dbao) config.getUserProperties().get("db");
        this.protocol = (Protocol) config.getUserProperties().get("protocol");
    }

    @OnMessage
    public void message(Session session, Message msg) {
        //query string is the client I want to communicate with
        String receiver = session.getQueryString();

        logger.log(Level.SEVERE, "HOPSWORKS: QUERY STRING {0}", session.getQueryString());
        logger.log(Level.SEVERE, "RECEIVED MESSAGE: {0}", msg.toString());

        if (!authenticate(receiver)) {
            this.sendError(session, "You are not logged in the system");
            return;
        }

        try {
            Message response = this.protocol.GFR(msg);

            //broadcast the response back to everybody
            for (Session sess : this.sessions) {
                sess.getBasicRemote().sendObject(response);
            }
        } catch (NullPointerException | EncodeException | IOException e) {

            logger.log(Level.SEVERE, e.getMessage(), e);
            this.sendError(session, e.getMessage());
        }
    }

    @OnClose
    public void onClose(Session session) {

        if (this.sessions.contains(session)) {
            // remove connection
            this.sessions.remove(session);

            logger.log(Level.SEVERE, "HOPSWORKS: USER {0} SESSION DESTROYED sessions {1}", 
                    new Object[]{sender, this.sessions.size()});

            this.broadcast(new TextMessage(this.sender, "Left"));
        }
    }

    /**
     * Broadcasts a message to all open websocket sessions
     *
     * @param msg the message to be sent
     */
    public void broadcast(Message msg) {
        synchronized (sessions) {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendObject(msg);
                }
            }
        }
    }

    @OnError
    public void error(Session session, Throwable t) {
        //t.printStackTrace();
        logger.log(Level.SEVERE, t.getMessage(), t);
    }

    private void sendClient(Session session, Message message) {
        try {
            session.getBasicRemote().sendObject(message);
        } catch (IOException | EncodeException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    private void addSession(Session session) {
        this.sessions.add(session);
        logger.log(Level.SEVERE, "HOPSWORKS: USER {0} CONNECTED sessid {1}", new Object[]{sender, session.getId()});
        logger.log(Level.SEVERE, "HOPSWORKS: CONNECTED USERS {0}", this.sessions.size());
    }

    private void removeSession(Session session) {
        this.sessions.remove(session);
    }

    private void sendError(Session session, String err) {
        String error = String.format("error: %s", err);
        Message message = new TextMessage("Server", error);
        message.setStatus("ERROR");
        this.sendClient(session, message);
    }

    private boolean authenticate(String username) {
//        HttpSession session = this.auth.getSession();
//        System.err.println("SESSSSSSION NULLL " + session == null);
//        System.err.println("SESSSSSSION ID " + session.getId());

        return true;
//        return session != null && auth.getUsername().equals(username);
    }
}
