package se.kth.meta.db;

import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.Field;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author vangelis
 */
@Stateless
public class FieldFacade extends AbstractFacade<Field> {

  private static final Logger logger = Logger.getLogger(FieldFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public FieldFacade() {
    super(Field.class);
  }

  public Field getField(int fieldid) throws DatabaseException {

    return this.em.find(Field.class, fieldid);
  }

  /**
   * adds a new record into 'fields' table. Each record represents a table
   * column with its attributes (searchable, required, maxsize) and it is
   * associated with the relevant table.
   *
   * @param field the field with its corresponding attributes
   * @return
   * @throws se.kth.meta.exception.DatabaseException
   */
  public int addField(Field field) throws DatabaseException {

    try {
      Field f = this.getField(field.getId());

      if (f != null && f.getId() != -1) {
        f.copy(field);
        this.em.merge(f);
      } else {
        f = field;
        /*
         * The field is a new one so it has id -1; all the child entities
         * (predefined values) are pointing to their parent (field) with id -1.
         * Database throws exception because there is no field with id -1.
         * So remove all the child entities from the field before persisting it
         * and handle the predefined values later
         */
        f.resetFieldPredefinedValues();
        this.em.persist(f);
      }

      this.em.flush();
      this.em.clear();
      return f.getId();
    } catch (IllegalStateException | SecurityException e) {
      throw new DatabaseException("Could not add field.", e);
    }
  }

  /**
   * Deletes a fields entity. If the object is an
   * unmanaged entity it has to be merged to become managed so that the delete
   * can cascade down its associations if necessary
   * <p>
   *
   * @param field the field object that's going to be re
   * @throws se.kth.meta.exception.DatabaseException when the field to be
   * deleted is associated to raw data
   */
  public void deleteField(Field field) throws DatabaseException {

    Field f = this.contains(field) ? field : this.getField(field.getId());

    if (this.em.contains(f)) {
      this.em.remove(f);
    } else {
      //if the object is unmanaged it has to be managed before it is removed
      this.em.remove(this.em.merge(f));
    }
    //persist the changes to the database immediately
    this.em.flush();
  }

  /**
   * Checks if a field instance is a managed entity
   * <p>
   * @param field
   * @return
   */
  public boolean contains(Field field) {
    return this.em.contains(field);
  }
}
