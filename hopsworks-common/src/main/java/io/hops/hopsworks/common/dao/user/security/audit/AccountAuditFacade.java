/*
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
 *
 */

package io.hops.hopsworks.common.dao.user.security.audit;

import io.hops.hopsworks.common.dao.AbstractFacade;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.servlet.http.HttpServletRequest;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.util.Settings;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.TypedQuery;

@Stateless
public class AccountAuditFacade extends AbstractFacade<AccountAudit> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  @EJB
  private Settings settings;

  private Set<String> whitelistUserLogins;

  public AccountAuditFacade() {
    super(AccountAudit.class);
  }
  
  @PostConstruct
  private void init() {
    whitelistUserLogins = new HashSet<>();
    String whitelist = settings.getWhitelistUsersLogin();
    String[] whitelistTokens = whitelist.split(",");
    Collections.addAll(whitelistUserLogins, whitelistTokens);
  }

  public Userlogins getLastUserLogin(Users user) {
    Query query = em.createNamedQuery("Userlogins.findUserLast", Userlogins.class)
        .setParameter("user", user)
        .setMaxResults(1);
    List<Userlogins> logins = query.getResultList();
    //A user might have never logged in, so we need to check first
    if (!logins.isEmpty()) {
      return logins.get(0);
    }
    return null;
  }

  public void registerLoginInfo(Users user, String action, String outcome, HttpServletRequest req) {
    if (!whitelistUserLogins.contains(user.getEmail())) {
      Userlogins userlogin = new Userlogins(req.getRemoteHost(), extractUserAgent(req), user,
          action, outcome, new Date());
      em.persist(userlogin);
    }
  }

  public void registerRoleChange(Users user, String action, String outcome,
          String message, Users targetUser, HttpServletRequest req) {
    RolesAudit rolesAudit = new RolesAudit(action, new Date(), message, extractUserAgent(req),
        req.getRemoteHost(), outcome, targetUser, user);
    em.persist(rolesAudit);
  }

  /**
   * Register account related changes.
   *
   * @param init
   * @param action
   * @param outcome
   * @param message
   * @param target
   * @param req
   */
  public void registerAccountChange(Users init, String action, String outcome,
          String message, Users target, HttpServletRequest req) {
    AccountAudit accountAudit = new AccountAudit(action, new Date(), message, outcome, req.getRemoteHost(),
        extractUserAgent(req), target, init);
    em.persist(accountAudit);
  }

  public List<AccountAudit> findByInitiator(Users user) {
    TypedQuery<AccountAudit> query = em.createNamedQuery("AccountAudit.findByInitiator", AccountAudit.class);
    query.setParameter("initiator", user);

    return query.getResultList();
  }

  public List<AccountAudit> findByTarget(Users user) {
    TypedQuery<AccountAudit> query = em.createNamedQuery("AccountAudit.findByTarget", AccountAudit.class);
    query.setParameter("target", user);

    return query.getResultList();
  }

  private String extractUserAgent(HttpServletRequest httpServletRequest) {
    String userAgent = httpServletRequest.getHeader("User-Agent");
    if (userAgent == null || userAgent.isEmpty()) {
      return "Unknown User-Agent";
    }

    return StringUtils.left(userAgent, 255);
  }
}
