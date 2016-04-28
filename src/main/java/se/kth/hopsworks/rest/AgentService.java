/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.rest;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import se.kth.hopsworks.util.PKIUtils;

/**
 *
 * @author jdowling
 */
@Path("/agent")
@Stateless
@RolesAllowed({"AGENT", "SYS_ADMIN"})
public class AgentService {

    final static Logger logger = Logger.getLogger(AgentService.class.getName());


    @PUT
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sign(@Context HttpServletRequest req, String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            boolean toRegister = false;
            String certificate = "no certificate";
            if (json.has("csr")) {
                String csr = json.getString("csr");
                certificate = PKIUtils.signWithServerCertificate(csr);
            }

            return Response.ok(certificate).build();
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Exception: {0}", ex);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

}
