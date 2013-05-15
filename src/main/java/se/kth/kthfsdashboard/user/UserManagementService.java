/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.user;

import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author Jim Dowling<jdowling@sics.se>
 */
@Path("/auth")
@Produces(MediaType.TEXT_PLAIN)
@Stateless
public class UserManagementService {
 
    @EJB
    private UserFacade userBean;
 
    @GET
    @Path("ping")
    public String ping() {
        return "alive";
    }
 
    @POST
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@FormParam("email") String email, @FormParam("password") String password,
            @Context HttpServletRequest req) {
         
        JsonResponse json = new JsonResponse();
         
        //only login if not already logged in...
        if(req.getUserPrincipal() == null){
            try {
                req.login(email, password);
                req.getServletContext().log("Authentication Demo: successfully logged in " + email);
            } catch (ServletException e) {
                e.printStackTrace();
                json.setStatus("FAILED");
                json.setErrorMsg("Authentication failed");
                return Response.ok().entity(json).build();
            }
        }else{
            req.getServletContext().log("Skip logged because already logged in: "+email);
        }
         
        //read the user data from db and return to caller
        json.setStatus("SUCCESS");
         
        Username user = userBean.findByEmail(email);
        req.getServletContext().log("Authentication Demo: successfully retrieved User Profile from DB for " + email);
        json.setData(user);
         
        //we don't want to send the hashed password out in the json response
        userBean.detach(user);
        user.setPassword(null);
        user.setGroups(null);
        return Response.ok().entity(json).build();
    }
 
    @GET
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@Context HttpServletRequest req) {
 
        JsonResponse json = new JsonResponse();
 
        try {
            req.logout();
            json.setStatus("SUCCESS");
            req.getSession().invalidate();
        } catch (ServletException e) {
            e.printStackTrace();
            json.setStatus("FAILED");
            json.setErrorMsg("Logout failed on backend");
        }
        return Response.ok().entity(json).build();
    }
 
    @POST
    @Path("register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Response register(UserDTO newUser, @Context HttpServletRequest req) {
 
        JsonResponse json = new JsonResponse();
        json.setData(newUser); //just return the date we received
 
        //do some validation (in reality you would do some more validation...)
        //by the way: i did not choose to use bean validation (JSR 303)
        if (newUser.getPassword1().length() == 0 || !newUser.getPassword1().equals(newUser.getPassword2())) {
            json.setErrorMsg("Both passwords have to be the same - typo?");
            json.setStatus("FAILED");
            return Response.ok().entity(json).build();
        }
 
        Username user = new Username(newUser);
 
        List<Group> groups = new ArrayList<Group>();
        groups.add(Group.ADMIN);
        groups.add(Group.USER);
        user.setGroups(groups);
 
        //this could cause a runtime exception, i.e. in case the user already exists
        //such exceptions will be caught by our ExceptionMapper, i.e. javax.transaction.RollbackException
        userBean.persist(user); // this would use the clients transaction which is committed after save() has finished
        req.getServletContext().log("successfully registered new user: '" + newUser.getEmail() + "':'" + newUser.getPassword1() + "'");
 
        req.getServletContext().log("execute login now: '" + newUser.getEmail() + "':'" + newUser.getPassword1() + "'");
        try {
            req.login(newUser.getEmail(), newUser.getPassword1());
            json.setStatus("SUCCESS");
        } catch (ServletException e) {
            e.printStackTrace();
            json.setErrorMsg("User Account created, but login failed. Please try again later.");
            json.setStatus("FAILED"); //maybe some other status? you can choose...
        }
         
        return Response.ok().entity(json).build();
    }
 
}
