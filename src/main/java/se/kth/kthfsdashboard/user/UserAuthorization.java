/*
 * Copyright (c) 2010, Oracle. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of Oracle nor the names of its contributors
 *   may be used to endorse or promote products derived from this software without
 *   specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package se.kth.kthfsdashboard.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.context.FacesContext;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

@ManagedBean
@RequestScoped
public class UserAuthorization  {

    /**
     * <p>The key for the session scoped attribute holding the appropriate
     * <code>Wuser</code> instance.</p>
     */
    public static final String USER_SESSION_KEY = "user";
    /**
     * <p>The
     * <code>PersistenceContext</code>.</p>
     */
    @PersistenceContext(unitName = "kthfsPU")
    private EntityManager em;

    @EJB
    private UserFacade userFacade;

    @Resource(name = "mail/jdowling")
    private Session mailSession;
    
    /**
     * <p>User properties.</p>
     */
    private String name;
    private String username;
    private String email;
    private String password;
    private String mobileNum;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMobileNum() {
        return mobileNum;
    }

    public void setMobileNum(String mobileNum) {
        this.mobileNum = mobileNum;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void sendMessage(Username ac) {
        Message msg = new MimeMessage(mailSession);
        try {
            msg.setFrom(new InternetAddress("jdowling@kth.se"));
            msg.setRecipient(Message.RecipientType.TO,
                    new InternetAddress(ac.getEmail()));
            msg.setRecipient(Message.RecipientType.BCC,
                    new InternetAddress("jdowling@sics.se"));
            msg.setSubject("ECPC registration confirmation");


            msg.setText("Hello " + ac.getEmail() + ",\n");

            msg.setHeader("X-Mailer", "My Mailer");
            java.util.Date timeStamp = new java.util.Date();
            msg.setSentDate(timeStamp);

            Transport.send(msg);
        } catch (MessagingException me) {
//            me.printStackTrace();
            System.out.println(me);
        }
    }

    // ---------------------------------------------------------- Public Methods
    /**
     * <p>Validates the user. If the user doesn't exist or the password is
     * incorrect, the appropriate message is added to the current
     * <code>FacesContext</code>. If the user successfully authenticates,
     * navigate them to the page referenced by the outcome
     * <code>app-main</code>. </p>
     *
     * @return
     * <code>app-main</code> if the user authenticates, otherwise returns
     * <code>null</code>
     */
    /**
     * @return
     * <code>login</code> if the user is created, otherwise returns
     * <code>null</code>
     */
    public String register() {
        Username reg = new Username();
        reg.setName(name);
        reg.setUsername(username);
        reg.setEmail(email);
        reg.setMobileNum(mobileNum);
        reg.setPassword(password);

        try {
            userFacade.create(reg);
            sendMessage(reg);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Successfully registered.", ""));
            return "finished";
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Error registering. "
                    + e.getMessage(), ""));
            return null;
        }
    }

    
    public String login() {
         FacesContext context = FacesContext.getCurrentInstance();  
         HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
         System.out.println(context == null);
         System.out.println(request == null);
        try {        
            request.login(this.username, this.password);        
           if(request.isUserInRole("admin")) {  
                return "/pages/protected/admin/index.xhtml?faces-redirect=true";  
           } else if(request.isUserInRole("professor")){  
                return "/pages/protected/professor/index.xhtml?faces-redirect=true";  
           }        
        } catch (ServletException e) {
            context.addMessage(null, new FacesMessage("Login failed."));
            return "error";
        }
        return "login.xhtml";
    }
 
    public void logout() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        try {
            request.logout();
        } catch (ServletException e) {
            context.addMessage(null, new FacesMessage("Logout failed."));
        }
    }    
    
    public String validateUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        Username admin = getUser();
        if (admin != null) {
            if (!admin.getPassword().equals(password)) {
                FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                        "Login Failed!",
                        "The password specified is not correct.");
                context.addMessage(null, message);
                return null;
            }
            context.getExternalContext().getSessionMap().put(USER_SESSION_KEY, admin);
            return "app-main";
        } else {
            FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Login Failed!",
                    "Username '"
                    + email
                    + "' does not exist.");
            context.addMessage(null, message);
            return null;
        }
    }

    public void create() {
        Username u = new Username();
        u.setEmail("jdowling@sics.se");
        u.setPassword("jim");
        u.setMobileNum("022");
        u.setName("jim Dowling");
        u.setSalt("011".getBytes());
        List<Group> g = new ArrayList<Group>();
        g.add(Group.USER);
        u.setGroups(g);
        userFacade.persist(u);
    }
    
    private Username getUser() {
        try {
            // support login using both email and username
            Username admin = (Username) 
                    em.createNamedQuery("Username.findByEmail").
                    setParameter("email", username).
                    setHint("eclipselink.refresh",true).
                    getSingleResult();
            if (admin == null) {
                    admin = (Username) 
                    em.createNamedQuery("Username.findByUsername").
                    setParameter("username", username).
                    setHint("eclipselink.refresh",true).
                    getSingleResult();                
            }
            return admin;
        } catch (NoResultException nre) {
            return null;
        }
    }


}
