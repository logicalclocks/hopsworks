/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.hops.hopsworks.remote.user.auth.ldap;

import io.hops.hopsworks.common.dao.remote.user.RemoteUser;
import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.util.Settings;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.naming.CompositeName;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.login.LoginException;

@Stateless
public class LdapRealm {
  
  private static final Logger LOGGER = Logger.getLogger(LdapRealm.class.getName());
  private static final String[] DN_ONLY = {"dn"};
  private static final String SUBST_SUBJECT_NAME = "%s";
  private static final String SUBST_SUBJECT_DN = "%d";
  private static final String JNDICF_DEFAULT = "com.sun.jndi.ldap.LdapCtxFactory";
  private static final String OBJECTGUID = "objectGUID";
  
  @EJB
  private Settings settings;
  @EJB
  private ObjectGUIDUtil objectGUIDUtil;
  
  private String entryUUIDField;
  private String usernameField;
  private String givenNameField;
  private String surnameField;
  private String emailField;
  private String searchFilter;
  private String groupSearchFilter;
  private String krbSearchFilter;
  private String groupTarget;
  private String baseDN;
  private String groupDN;
  private String dynamicGroupTarget;
  private String[] returningAttrs;
  private Hashtable ldapProperties;
  
  @Resource(lookup = "ldap/LdapResource")
  private DirContext dirContext;
  
  @PostConstruct
  public void init() {
    if (Boolean.parseBoolean(settings.getLDAPAuthStatus()) == false && Boolean.parseBoolean(settings.getKRBAuthStatus())
      == false) {
      throw new IllegalStateException("LDAP not enabled.");
    }
    ldapProperties = getLdapBindProps();
    String attrBinary = settings.getLdapAttrBinary();
    entryUUIDField = (String) ldapProperties.get(attrBinary);
    if (entryUUIDField == null || entryUUIDField.isEmpty()) {
      throw new IllegalStateException("No UUID set for resource. Try setting " + attrBinary);
    }
    populateVars();
  }
  
  private void populateVars() {
    usernameField = settings.getLdapUserId();
    givenNameField = settings.getLdapUserGivenName();
    surnameField = settings.getLdapUserSurname();
    emailField = settings.getLdapUserMail();
    searchFilter = settings.getLdapUserSearchFilter();
    groupSearchFilter = settings.getLdapGroupSearchFilter();
    krbSearchFilter = settings.getKrbUserSearchFilter();
    groupTarget = settings.getLdapGroupTarget();
    baseDN = settings.getLdapUserDN();
    groupDN = settings.getLdapGroupDN();
    dynamicGroupTarget = settings.getLdapDynGroupTarget();
    String[] attrs = {entryUUIDField, usernameField, givenNameField, surnameField, emailField};
    returningAttrs = attrs;
  }
  
  /**
   * Find ldap user and try to login, if login succeed gets user attributes from ldap.
   *
   * @param username
   * @param password
   * @return
   * @throws LoginException
   * @throws javax.naming.NamingException
   */
  public RemoteUserDTO findAndBind(String username, String password) throws LoginException, NamingException {
    if (password == null) {
      throw new LoginException("Password not set.");
    }
    return findRemoteUser(searchFilter, username, password);
  }
  
  /**
   * Find krb user from ldap
   *
   * @param principalName
   * @return
   * @throws NamingException
   * @throws javax.security.auth.login.LoginException
   */
  public RemoteUserDTO findKrbUser(String principalName) throws NamingException, LoginException {
    return findRemoteUser(krbSearchFilter, principalName, null);
  }
  
  /**
   * Authenticate user with ldap
   *
   * @param username
   * @param password
   * @throws LoginException
   * @throws javax.naming.NamingException
   */
  public void authenticateLdapUser(String username, String password) throws LoginException, NamingException {
    String userId = getUserId(username, searchFilter);
    String userDN = getUserDN(userId);
    bindAsUser(userDN, password); // try login
  }
  
  private RemoteUserDTO findRemoteUser(String searchFilter, String username, String pwd) throws NamingException,
    LoginException {
    String userId = getUserId(username, searchFilter);
    String userDN = getUserDN(userId);
    if (pwd != null) {
      bindAsUser(userDN, pwd); // if ldap try login
    }
    RemoteUserDTO user = createLdapUser(userId);
    user.setEmailVerified(true);
    user.setGroups(getUserGroups(username, searchFilter));
    validateLdapUser(user);
    return user;
  }
  
  private String getUserId(String username, String searchFilter) {
    populateVars();
    StringBuffer sb = new StringBuffer(searchFilter);
    substitute(sb, SUBST_SUBJECT_NAME, username);
    return sb.toString();
  }
  
  private String getUserDN(String userId) throws LoginException, NamingException {
    String userDN = userDNSearch(userId);
    if (userDN == null) {
      throw new LoginException("User not found.");
    }
    return userDN;
  }
  
  /**
   * Authenticate user with ldap
   *
   * @param user
   * @param pwd
   * @throws LoginException
   * @throws javax.naming.NamingException
   */
  public RemoteUserDTO getLdapUser(RemoteUser user, String pwd) throws LoginException, NamingException {
    populateVars();
    String id = OBJECTGUID.equals(entryUUIDField) ? objectGUIDUtil.convertToByteString(user.getUuid()) : user.getUuid();
    String userId = entryUUIDField + "=" + id;
    String userDN = userDNSearch(userId);
    if (userDN == null) {
      throw new LoginException("User not found.");
    }
    bindAsUser(userDN, pwd); // try login
    RemoteUserDTO remoteUser = createLdapUser(userId);
    remoteUser.setEmailVerified(true);
    remoteUser.setGroups(getUserGroups(remoteUser.getUid(), searchFilter));//uses search filter for uid/(sAMAccountName)
    validateLdapUser(remoteUser);
    return remoteUser;
  }
  
  /**
   * Get user group
   *
   * @param username
   * @return
   * @throws NamingException
   */
  public List<String> getUserGroups(String username, String searchFilter) throws NamingException {
    populateVars();
    StringBuffer sb = new StringBuffer(searchFilter);
    substitute(sb, SUBST_SUBJECT_NAME, username);
    String userid = sb.toString();
    String userDN = userDNSearch(userid);
    if (userDN == null) {
      throw new IllegalArgumentException("User not found.");
    }
    sb = new StringBuffer(groupSearchFilter);
    StringBuffer dynsb = new StringBuffer(searchFilter);
    substitute(sb, SUBST_SUBJECT_NAME, username);
    substitute(sb, SUBST_SUBJECT_DN, userDN);
    substitute(dynsb, SUBST_SUBJECT_NAME, username);
    substitute(dynsb, SUBST_SUBJECT_DN, userDN);
    String srchFilter = sb.toString();
    String dynSearchFilter = dynsb.toString();
    List<String> groupsList = new ArrayList<>();
    groupsList.addAll(groupSearch(srchFilter));
    groupsList.addAll(dynamicGroupSearch(dynSearchFilter));
    return groupsList;
  }
  
  private String userDNSearch(String filter) throws NamingException {
    String distinguishedName = null;
    NamingEnumeration answer = null;
    
    SearchControls ctls = new SearchControls();
    ctls.setReturningAttributes(DN_ONLY);
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    ctls.setCountLimit(1);
    
    try {
      answer = dirContext.search(baseDN, filter, ctls);
      if (answer.hasMore()) {
        SearchResult res = (SearchResult) answer.next();
        CompositeName compDN = new CompositeName(res.getNameInNamespace());
        distinguishedName = compDN.get(0);
      }
    } catch (InvalidNameException ex) {
      LOGGER.log(Level.WARNING, "Ldaprealm search error: {0}", filter);
      LOGGER.log(Level.WARNING, "Ldaprealm security exception: {0}", ex.toString());
    } finally {
      if (answer != null) {
        try {
          answer.close();
        } catch (Exception ex) {
        
        }
      }
    }
    return distinguishedName;
  }
  
  private RemoteUserDTO createLdapUser(String filter) {
    NamingEnumeration answer = null;
    RemoteUserDTO ldapUserDTO = null;
    SearchControls ctls = new SearchControls();
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    ctls.setReturningAttributes(returningAttrs);
    ctls.setCountLimit(1);
    try {
      answer = dirContext.search(baseDN, filter, ctls);
      if (answer.hasMore()) {
        SearchResult res = (SearchResult) answer.next();
        Attributes attrs = res.getAttributes();
        ldapUserDTO = new RemoteUserDTO(getUUIDAttribute(attrs, entryUUIDField), getAttribute(attrs, usernameField),
          getAttribute(attrs, givenNameField), getAttribute(attrs, surnameField), getAttrList(attrs, emailField),
          true);
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Ldaprealm search error: {0}", filter);
      LOGGER.log(Level.WARNING, "Ldaprealm security exception: {0}", e.toString());
    } finally {
      if (answer != null) {
        try {
          answer.close();
        } catch (Exception ex) {
        }
      }
    }
    return ldapUserDTO;
  }
  
  private void bindAsUser(String bindDN, String password) throws LoginException {
    Hashtable<String, String> p = getLdapBindProps();
    p.put(Context.INITIAL_CONTEXT_FACTORY, JNDICF_DEFAULT);
    p.put(Context.SECURITY_PRINCIPAL, bindDN);
    p.put(Context.SECURITY_CREDENTIALS, password);
    DirContext ctx = null;
    try {
      ctx = new InitialDirContext(p);
    } catch (Exception e) {
      LOGGER.log(Level.INFO, "Error binding to directory as: {0}", bindDN);
      LOGGER.log(Level.INFO, "Exception from JNDI: {0}", e.toString());
      throw new LoginException(e.getMessage());
    } finally {
      if (ctx != null) {
        try {
          ctx.close();
        } catch (Exception e) {
        }
      }
    }
  }
  
  private List<String> groupSearch(String searchFilter) {
    List<String> groupList = new ArrayList<>();
    String[] targets = new String[]{groupTarget};
    try {
      SearchControls ctls = new SearchControls();
      ctls.setReturningAttributes(targets);
      ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      NamingEnumeration e = dirContext.search(groupDN, searchFilter.replaceAll(Matcher.quoteReplacement("\\"),
        Matcher.quoteReplacement("\\\\")), ctls);
      
      while (e.hasMoreElements()) {
        SearchResult result = (SearchResult) e.next();
        Attribute grpAttr = result.getAttributes().get(groupTarget);
        for (int i = 0; i < grpAttr.size(); i++) {
          groupList.add((String) grpAttr.get(i));
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error in group search: {0}", searchFilter);
    }
    return groupList;
  }
  
  private List<String> dynamicGroupSearch(String dynSearchFilter) {
    List<String> groupList = new ArrayList<>();
    String[] targets = new String[]{dynamicGroupTarget};
    try {
      SearchControls ctls = new SearchControls();
      ctls.setReturningAttributes(targets);
      ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      ctls.setReturningObjFlag(false);
      
      NamingEnumeration e = dirContext.search(groupDN, dynSearchFilter, ctls);
      while (e.hasMoreElements()) {
        SearchResult result = (SearchResult) e.next();
        Attribute isMemberOf = result.getAttributes().get(dynamicGroupTarget);
        if (isMemberOf != null) {
          for (Enumeration values = isMemberOf.getAll(); values.hasMoreElements(); ) {
            String grpDN = (String) values.nextElement();
            LdapName dn = new LdapName(grpDN);
            for (Rdn rdn : dn.getRdns()) {
              if (rdn.getType().equalsIgnoreCase(groupTarget)) {
                groupList.add((String) rdn.getValue());
                break;
              }
            }
          }
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error in dynamic group search: {0}", dynSearchFilter);
    }
    return groupList;
  }
  
  private String getUUIDAttribute(Attributes attrs, String key) throws NamingException {
    Attribute attr = attrs.remove(key);
    byte[] guid = attr != null ? (byte[]) attr.get() : "".getBytes();
    String id = OBJECTGUID.equals(entryUUIDField) ? objectGUIDUtil.convertToDashedString(guid) : new String(guid);
    return id;
  }
  
  private String getAttribute(Attributes attrs, String key) throws NamingException {
    Attribute attr = attrs.remove(key);
    return attr != null ? (String) attr.get() : "";
  }
  
  private List<String> getAttrList(Attributes attrs, String key) throws NamingException {
    List<String> vals = new ArrayList<>();
    Attribute attr = attrs.remove(key);
    if (attr == null) {
      return vals;
    }
    NamingEnumeration a = attr.getAll();
    while (a.hasMore()) {
      vals.add((String) a.next());
    }
    return vals;
  }
  
  private Hashtable getLdapBindProps() {
    Hashtable ldapProperties = new Hashtable();
    try {
      ldapProperties = (Hashtable) dirContext.getEnvironment().clone();
    } catch (NamingException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    return ldapProperties;
  }
  
  private static void substitute(StringBuffer sb, String target, String value) {
    int i = sb.indexOf(target);
    while (i >= 0) {
      sb.replace(i, i + target.length(), value);
      i = sb.indexOf(target);
    }
  }
  
  private void validateLdapUser(RemoteUserDTO user) throws LoginException {
    if (user.getUuid() == null || user.getUuid().isEmpty()) {
      throw new LoginException("Could not find UUID for Ldap user.");
    }
    if (user.getEmail() == null || user.getEmail().isEmpty()) {
      throw new LoginException("Could not find email for Ldap user.");
    }
    if (user.getGivenName() == null || user.getGivenName().isEmpty()) {
      throw new LoginException("Could not find givenName for Ldap user.");
    }
    if (user.getSurname() == null || user.getSurname().isEmpty()) {
      throw new LoginException("Could not find surname for Ldap user.");
    }
    if (!user.isEmailVerified()) {
      throw new LoginException("User email not yet verified.");
    }
    
  }
  
}
