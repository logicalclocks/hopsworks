package se.kth.hopsworks.message;

import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import se.kth.hopsworks.user.model.Users;
import se.kth.kthfsdashboard.user.AbstractFacade;

@Stateless
public class MessageFacade extends AbstractFacade<Message> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public MessageFacade() {
    super(Message.class);
  }
  /**
   * Get a message by id
   * @param id
   * @return 
   */
  public Message findMessageById(int id) {
    return em.find(Message.class, id);
  }
  
  /**
   * Get all messages to a user
   * @param user
   * @return list of message
   */
  public List<Message> getAllMessagesTo(Users user){
    TypedQuery<Message> query = em.createNamedQuery("Message.findByToAndDeleted",
            Message.class)
            .setParameter("to", user)
            .setParameter("deleted", false);
    return query.getResultList();
  }
  
  /**
   * Get all unread messages to a user
   * @param user
   * @return list of message
   */
  public List<Message> getAllUnreadMessagesTo(Users user){
    TypedQuery<Message> query = em.createNamedQuery("Message.findMessagesByToAndUnread",
            Message.class)
            .setParameter("to", user)
            .setParameter("unread", true);
    return query.getResultList();
  }
  
  /**
   * Gets the count of all messages to a user
   * @param user
   * @return number of messages
   */
  public Long countAllMessagesTo(Users user) {
    TypedQuery query = em.createNamedQuery("Message.countByToAndDeleted",
            Long.class)
            .setParameter("to", user)
            .setParameter("deleted", false);
    try {
      return (Long)query.getSingleResult();
    }catch (NoResultException e){
      return 0L;
    }
  }
  /**
   * Gets the count of unread messages to a user
   * @param user
   * @return number of messages
   */
  public Long countUnreadMessagesTo(Users user) {
    TypedQuery query = em.createNamedQuery("Message.countByToAndUnread",
            Long.class)
            .setParameter("to", user)
            .setParameter("unread", true);
    try {
      return (Long)query.getSingleResult();
    }catch (NoResultException e){
      return 0L;
    }
  }
  /**
   * Gets all deleted messages to a user
   * @param user
   * @return list of messages
   */
  public List<Message> getAllDeletedMessagesTo(Users user){
    TypedQuery<Message> query = em.createNamedQuery("Message.findByToAndDeleted",
            Message.class)
            .setParameter("to", user)
            .setParameter("deleted", true);
    return query.getResultList();
  }
  /**
   * Gets the count of deleted messages to a user
   * @param user
   * @return number of messages
   */
  public Long countDeletedMessagesTo(Users user) {
    TypedQuery query = em.createNamedQuery("Message.countByToAndDeleted",
            Long.class)
            .setParameter("to", user)
            .setParameter("deleted", true);
    try {
      return (Long)query.getSingleResult();
    }catch (NoResultException e){
      return 0L;
    }
  }
  /**
   * Empty trash 
   * @param user
   * @return 
   */
  public int emptyTrash(Users user) {
    TypedQuery query = em.createNamedQuery("Message.emptyToAndDeleted",
            Long.class)
            .setParameter("to", user)
            .setParameter("deleted", true);
    try {
      return query.executeUpdate();
    }catch (NoResultException e){
      return 0;
    }
  }
  /**
   * Empty messages folder
   * @param user
   * @return 
   */
  public int emptyMessage(Users user) {
    TypedQuery query = em.createNamedQuery("Message.emptyToAndDeleted",
            Long.class)
            .setParameter("to", user)
            .setParameter("deleted", false);
    try {
      return query.executeUpdate();
    }catch (NoResultException e){
      return 0;
    }
  }

}
