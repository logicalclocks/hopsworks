/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.remote.user.ldap;

import io.hops.hopsworks.common.remote.RemoteUserDTO;
import io.hops.hopsworks.common.remote.RemoteUsersDTO;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.persistence.entity.remote.user.RemoteUser;

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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

@Stateless
public class LdapRealm {
  
  private static final Logger LOGGER = Logger.getLogger(LdapRealm.class.getName());
  private static final String[] DN_ONLY = {"dn"};
  private static final String SUBST_SUBJECT_NAME = "%s";
  private static final String SUBST_SUBJECT_DN = "%d";
  private static final String SUBST_SUBJECT_CN = "%c";
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
  
  @Resource(name = "ldap/LdapResource")
  private DirContext dirContext;
  
  @PostConstruct
  public void init() {
    if (!settings.isLdapEnabled() && !settings.isKrbEnabled()) {
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
   * @throws NamingException
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
   * @throws LoginException
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
   * @throws NamingException
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
   * @throws NamingException
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
  
  public List<String> getUserGroups(RemoteUser user) throws NamingException {
    String id = OBJECTGUID.equals(entryUUIDField) ? objectGUIDUtil.convertToByteString(user.getUuid()) : user.getUuid();
    String userId = entryUUIDField + "=" + id;
    String userDN = searchUserDN(userId);
    if (userDN == null) {
      return new ArrayList<>();
    }
    return getUserGroups(userDN);
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
  
  private List<String> getUserGroups(String userDN) {
    StringBuffer sb = new StringBuffer(groupSearchFilter);
    StringBuffer dynsb = new StringBuffer(searchFilter);
    substitute(sb, SUBST_SUBJECT_DN, userDN);
    substitute(dynsb, SUBST_SUBJECT_DN, userDN);
    String srchFilter = sb.toString();
    String dynSearchFilter = dynsb.toString();
    List<String> groupsList = new ArrayList<>();
    groupsList.addAll(groupSearch(srchFilter));
    groupsList.addAll(dynamicGroupSearch(dynSearchFilter));
    return groupsList;
  }

  private String searchUserDN(String filter) throws NamingException {
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
  
  private String userDNSearch(String filter) throws NamingException {
    try {
      return searchUserDN(filter);
    } catch (InvalidNameException ex) {
      LOGGER.log(Level.WARNING, "Ldap realm search error: {0}", filter);
      LOGGER.log(Level.WARNING, "Ldap realm security exception: {0}", ex.toString());
    }
    return null;
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
    List<String> groupList = null;
    String sf = searchFilter.replaceAll(Matcher.quoteReplacement("\\"), Matcher.quoteReplacement("\\\\"));
    try {
      groupList = ldapSearch(sf, groupTarget, groupDN);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error in group search: {0}", searchFilter);
    }
    return groupList;
  }
  
  private List<String> dynamicGroupSearch(String dynSearchFilter) {
    List<String> groupList = new ArrayList<>();
    String[] targets = new String[]{dynamicGroupTarget};
    NamingEnumeration e = null;
    try {
      SearchControls ctls = new SearchControls();
      ctls.setReturningAttributes(targets);
      ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
      ctls.setReturningObjFlag(false);
      
      e = dirContext.search(groupDN, dynSearchFilter, ctls);
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
    } catch (Exception ex) {
      LOGGER
        .log(Level.WARNING, "Error in dynamic group search: {0}. {1}", new Object[]{dynSearchFilter, ex.getMessage()});
    } finally {
      if (e != null) {
        try {
          e.close();
        } catch (Exception ex) {
        }
      }
    }
    return groupList;
  }
  
  public List<String> getAllGroups(String searchFilter) {
    List<String> groupList = null;
    try {
      groupList = ldapSearch(searchFilter, groupTarget, groupDN);
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error in get all groups search: {0}", searchFilter);
    }
    return groupList;
  }
  
  private List<String> ldapSearch(String searchFilter, String target, String dn) throws NamingException {
    List<String> resultList = new ArrayList<>();
    String[] targets = new String[]{target};
    SearchControls ctls = new SearchControls();
    ctls.setReturningAttributes(targets);
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    NamingEnumeration e = null;
    try {
      e = dirContext.search(dn, searchFilter, ctls);
      while (e.hasMoreElements()) {
        SearchResult result = (SearchResult) e.next();
        Attribute grpAttr = result.getAttributes().get(target);
        for (int i = 0; i < grpAttr.size(); i++) {
          resultList.add((String) grpAttr.get(i));
        }
      }
    } finally {
      if (e != null) {
        try {
          e.close();
        } catch (Exception ex) {
        }
      }
    }
    return resultList;
  }
  
  public RemoteUsersDTO getAllMembers(List<String> groups) throws NamingException {
    RemoteUsersDTO remoteUsersDTO = new RemoteUsersDTO();
    for (String group : groups) {
      remoteUsersDTO.addAll(getAllMembers(group));
    }
    return remoteUsersDTO;
  }
  
  public RemoteUsersDTO getAllMembers(String groupName) throws NamingException {
    RemoteUsersDTO remoteUsersDTO = new RemoteUsersDTO();
    String groupSearch = settings.getLdapGroupsSearchFilter().replace(SUBST_SUBJECT_CN, groupName);
    List<String> resultList = ldapSearch(groupSearch, settings.getLdapGroupsTarget(), groupDN);
    if (resultList == null || resultList.isEmpty()) {
      return remoteUsersDTO;
    }
    String dn = resultList.get(0);
    String memberOfQuery = settings.getLdapGroupMembersFilter().replace(SUBST_SUBJECT_DN, dn);
    SearchControls ctls = new SearchControls();
    ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    ctls.setReturningAttributes(returningAttrs);
    NamingEnumeration e = null;
    try {
      e = dirContext.search(groupDN, memberOfQuery, ctls);
      while (e.hasMoreElements()) {
        SearchResult result = (SearchResult) e.next();
        Attributes attrs = result.getAttributes();
        remoteUsersDTO.add(new RemoteUserDTO(getUUIDAttribute(attrs, entryUUIDField), getAttribute(attrs,
          usernameField), getAttribute(attrs, givenNameField), getAttribute(attrs, surnameField), getAttrList(attrs,
          emailField), true));
      }
    } finally {
      if (e != null) {
        try {
          e.close();
        } catch (Exception ex) {
        }
      }
    }
    return remoteUsersDTO;
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
  
  private void substitute(StringBuffer sb, String target, String value) {
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
