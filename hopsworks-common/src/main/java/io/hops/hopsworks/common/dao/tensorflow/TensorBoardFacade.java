package io.hops.hopsworks.common.dao.tensorflow;

import io.hops.hopsworks.common.dao.project.Project;
import io.hops.hopsworks.common.metadata.exception.DatabaseException;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.validation.ConstraintViolationException;
import java.util.List;

@Stateless
public class TensorBoardFacade {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  protected EntityManager getEntityManager() {
    return em;
  }

  public TensorBoardFacade() throws Exception {

  }

  public void persist(TensorBoard tensorBoard) throws DatabaseException {
    try {
      em.persist(tensorBoard);
      em.flush();
    } catch (ConstraintViolationException cve) {
      throw new DatabaseException("Could not update TensorBoard", cve);
    }
  }

  public void update(TensorBoard tensorBoard) throws DatabaseException {
    try {
      em.merge(tensorBoard);
      em.flush();
    } catch (ConstraintViolationException cve) {
      throw new DatabaseException("Could not update TensorBoard", cve);
    }
  }

  public void remove(TensorBoard tensorBoard) throws DatabaseException {
    try {
      TensorBoard managedTfServing = em.find(TensorBoard.class, tensorBoard.getTensorBoardPK());
      em.remove(em.merge(managedTfServing));
      em.flush();
    } catch (SecurityException | IllegalStateException ex) {
      throw new DatabaseException("Could not delete TensorBoard " + tensorBoard.getTensorBoardPK(), ex);
    }
  }

  public List<TensorBoard> findAll() {
    TypedQuery<TensorBoard> q = em.createNamedQuery("TensorBoard.findAll", TensorBoard.class);
    return q.getResultList();
  }

  public TensorBoard findForProjectAndUser(Project project, String email) throws DatabaseException {
    try {
      TypedQuery<TensorBoard> q = em.createNamedQuery("TensorBoard.findByProjectAndUser", TensorBoard.class);
      q.setParameter("projectId", project.getId());
      q.setParameter("email", email);
      TensorBoard tb = q.getSingleResult();
      return tb;
    } catch (NoResultException nre) {
      throw new DatabaseException("Could not retrieve running TensorBoard", nre);
    }
  }

  public List<TensorBoard> findForUser(String email) {
    TypedQuery<TensorBoard> q = em.createNamedQuery("TensorBoard.findByTeamMember", TensorBoard.class);
    q.setParameter("email", email);
    return q.getResultList();
  }

  public List<TensorBoard> findForProject(Project project) {
    TypedQuery<TensorBoard> q = em.createNamedQuery("TensorBoard.findByProjectId", TensorBoard.class);
    q.setParameter("projectId", project.getId());
    return q.getResultList();
  }
}
