package se.kth.hopsworks.meta.db;

import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.hopsworks.meta.entity.FieldPredefinedValue;
import se.kth.hopsworks.meta.exception.DatabaseException;

/**
 *
 * @author vangelis
 */
@Stateless
public class FieldPredefinedValueFacade extends AbstractFacade<FieldPredefinedValue> {

  private static final Logger logger = Logger.getLogger(
          FieldPredefinedValueFacade.class.getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public FieldPredefinedValueFacade() {
    super(FieldPredefinedValue.class);
  }

  public void addFieldPredefinedValue(FieldPredefinedValue value) throws
          DatabaseException {
    try {
      this.em.persist(value);
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException("Could not add predefined value " + value, e);
    }
  }

  /**
   * Deletes a field's predefined values. When a field modification happens
   * all its previously defined values need to be purged before the new
   * ones take their place i.e. a field gets its type changed from a dropdown
   * list to true/false, or to plain text
   * <p>
   *
   * @param fieldid
   * @throws se.kth.hopsworks.meta.exception.DatabaseException when an error happens
   */
  public void deleteFieldPredefinedValues(int fieldid) throws DatabaseException {

    Query query = this.em.
            createNamedQuery("FieldPredefinedValue.findByFieldid");
    query.setParameter("fieldid", fieldid);
    List<FieldPredefinedValue> valueList = query.getResultList();

    for (FieldPredefinedValue value : valueList) {
      if (this.em.contains(value)) {
        this.em.remove(value);
      } else {
        //if the object is unmanaged it has to be managed before it is removed
        this.em.remove(this.em.merge(value));
      }
    }
  }

}