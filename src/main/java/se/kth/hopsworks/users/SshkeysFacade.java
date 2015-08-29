package se.kth.hopsworks.users;

import se.kth.hopsworks.user.model.SshKeys;
import se.kth.hopsworks.user.model.SshKeysPK;
import se.kth.kthfsdashboard.user.AbstractFacade;

import javax.ejb.Stateless;
import javax.persistence.*;
import java.util.List;

@Stateless
public class SshkeysFacade extends AbstractFacade<SshKeys> {

  private final int STARTING_USER = 1000;

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public SshkeysFacade() {
    super(SshKeys.class);
  }

  public List<SshKeys> findAllById() {
    TypedQuery<SshKeys> query = em.createNamedQuery("SshKeys.findByUid",
            SshKeys.class);
    return query.getResultList();
  }

  public void persist(SshKeys user) {
    em.persist(user);
  }

  public void update(SshKeys user) {
    em.merge(user);
  }

  public void removeByIdName(int uid, String name) {
    SshKeysPK pk = new SshKeysPK(uid, name);
    if (pk != null) {
      SshKeys sshKey = new SshKeys(pk);
      em.remove(sshKey);
    }
  }

  @Override
  public void remove(SshKeys sshKey) {
    if (sshKey != null && sshKey.getSshKeysPK() != null && em.contains(sshKey)) {
      em.remove(sshKey);
    }
  }

  public void detach(SshKeys key) {
    em.detach(key);
  }

  /**
   * Get all sshkeys with user UID
   *
   * @param uid
   * @return
   */
  public List<SshKeys> findAllByUid(int uid) {
    TypedQuery<SshKeys> query = em.createNamedQuery("SshKeys.findByUid",
            SshKeys.class);
    query.setParameter("uid", uid);
    return query.getResultList();
  }

}
