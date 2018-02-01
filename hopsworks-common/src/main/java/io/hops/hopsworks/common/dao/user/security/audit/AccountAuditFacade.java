/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
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
