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

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>This
 * <code>PhaseListener</code> will be take action before the
 * <code>Restore View</code> phase is invoked. This allows us to check to see if
 * the user is logged in before allowing them to request a secure resource. If
 * the user isn't logged in, then the listener will move the user to the login
 * page.</p>
 *
 * @author rlubke
 */
public class AuthenticationPhaseListener implements PhaseListener {

    /**
     * <p>The outcome to trigger navigation to the login page.</p>
     */
    private static final String USER_LOGIN_OUTCOME = "login";

    // ---------------------------------------------- Methods from PhaseListener
    /**
     * <p>Determines if the user is authenticated. If not, direct the user to
     * the login view, otherwise all the user to continue to the requested
     * view.</p>
     *
     * <p>Implementation Note: We do this in the
     * <code>afterPhase</code> to make use of the
     * <code>NavigationHandler</code>.</p>
     */
    public void afterPhase(PhaseEvent event) {
        FacesContext context = event.getFacesContext();

        if (!userExists(context)) {
            // send the user to the login view
            if (requestingSecureView(context)) {
                context.responseComplete();
                context.getApplication().
                        getNavigationHandler().handleNavigation(context,
                        null,
                        USER_LOGIN_OUTCOME);
            }
        } // else - allow processing of the requested page
    }

    /**
     * <p>This is a no-op.</p>
     */
    public void beforePhase(PhaseEvent event) {
    }

    /**
     * @return <code>PhaseId.RESTORE_VIEW</code>
     */
    public PhaseId getPhaseId() {
        return PhaseId.RESTORE_VIEW;
    }

    // --------------------------------------------------------- Private Methods       
    /**
     * <p>Determine if the user has been authenticated by checking the session
     * for an existing
     * <code>Wuser</code> object.</p>
     *
     * @param context the <code>FacesContext</code> for the current request
     * @return <code>true</code> if the user has been authenticated, otherwise
     * <code>false</code>
     */
    private boolean userExists(FacesContext context) {
        ExternalContext extContext = context.getExternalContext();
    
//HttpServletRequest request = (HttpServletRequest) extContext.getRequest();
//try {
//    request.login(username, password);
//    return "home";
//} catch (ServletException e) {
//    errorMessage = e.getMessage();
//    return "error";
//}        
        return (extContext.getSessionMap().containsKey(UserAuthorization.USER_SESSION_KEY));
    }

    /**
     * <p>Determines if the requested view is one of the login pages which will
     * allow the user to access them without being authenticated.</p>
     *
     * <p>Note, this implementation most likely will not work if the
     * <code>FacesServlet</code> is suffix mapped.</p>
     *
     * @param context the <code>FacesContext</code> for the current request
     * @return <code>true</code> if the requested view is allowed to be accessed
     * without being authenticated, otherwise <code>false</code>
     */
    private boolean requestingSecureView(FacesContext context) {
        ExternalContext extContext = context.getExternalContext();
        String path = extContext.getRequestPathInfo();
        boolean onLoginPage = (-1 != context.getViewRoot().getViewId().
                lastIndexOf("index")) ? true : false;
//        FacesContext.getCurrentInstance().addMessage(null,
//                new FacesMessage(FacesMessage.SEVERITY_INFO, "Path: " 
//                + context.getViewRoot().getViewId(), ""));

//        return (!"/index.html".equals(path) 
//                && !"/".equals(path) 
//                && path!=null 
//                );              
        return !onLoginPage;
    }
}
