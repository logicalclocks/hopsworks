package se.kth.meta.db;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import se.kth.meta.entity.FieldPredefinedValue;
import se.kth.meta.entity.FieldType;
import se.kth.meta.entity.Field;
import se.kth.meta.entity.RawData;
import se.kth.meta.entity.MTable;
import se.kth.meta.entity.Template;
import se.kth.meta.entity.TupleToFile;
import se.kth.meta.exception.DatabaseException;

/**
 * Offers database functionalities
 *
 * @author Vangelis
 */
@RequestScoped
@PersistenceContext(unitName = "kthfsPU",
        name = "persistence/em")
@Resource(type = javax.transaction.UserTransaction.class,
        name = "UserTransaction")
public class Dbao {

  private static final Logger logger = Logger.getLogger(Dbao.class.getName());

  private Context ic;
  private EntityManager em;
  private UserTransaction utx;

  public Dbao() throws DatabaseException {

    try {
      this.ic = (Context) new InitialContext();
      this.em = (EntityManager) ic.lookup("java:comp/env/persistence/em");
      this.utx = (UserTransaction) ic.lookup("java:comp/env/UserTransaction");
    } catch (NamingException | IllegalStateException | SecurityException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new DatabaseException(Dbao.class.getName(), ex.getMessage());
    }
  }

  /**
   * adds a new record into 'tables' table. Represents a new metadata template
   * <p>
   *
   * @param table
   * @return the id of the newly inserted table or -1 in case of error
   * @throws se.kth.meta.exception.DatabaseException
   */
  public int addTable(MTable table) throws DatabaseException {

    try {
      this.utx.begin();
      MTable t = getTable(table.getId());

      if (t != null) {
        /*
         * if the table exists just update it, along with its corresponding
         * child fields.
         * Merge and cascade type ALL takes care of it, but first we need to
         * copy the incoming table
         * object into the managed object t
         */
        t.copy(table);
        this.em.merge(t);
      } else {
        /*
         * if the table is new then jpa cannot cascade insert to the child
         * fields.
         * we need to remove the fields in order for the table to be inserted
         * first
         * and acquire an id
         */
        t = table;
        t.resetFields();
        this.em.persist(t);
      }

      this.utx.commit();
      //this.em.clear();
      return t.getId();
    } catch (NotSupportedException | IllegalStateException | SecurityException |
            HeuristicMixedException |
            HeuristicRollbackException | RollbackException |
            SystemException e) {

      throw new DatabaseException(Dbao.class.getName(), e.getMessage());
    }
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
      this.utx.begin();
      Field f = getField(field.getId());
      if (f != null) {
        f.copy(field);
        this.em.merge(f);
      } else {
        f = field;
        f.resetFieldPredefinedValues();
        this.em.persist(f);
      }

      this.utx.commit();

      return f.getId();
    } catch (IllegalStateException | SecurityException | HeuristicMixedException |
            HeuristicRollbackException | NotSupportedException |
            RollbackException |
            SystemException e) {

      throw new DatabaseException(Dbao.class.getName(), "Could not add field "
              + e.getMessage());
    }
  }

  public void addFieldPredefinedValue(FieldPredefinedValue value) throws
          DatabaseException {
    try {
      this.utx.begin();
      this.em.persist(value);
      this.utx.commit();
    } catch (IllegalStateException | SecurityException | HeuristicMixedException |
            HeuristicRollbackException | NotSupportedException |
            RollbackException |
            SystemException e) {

      throw new DatabaseException(Dbao.class.getName(),
              "Could not add predefined value " + e.getMessage());
    }
  }

  public MTable getTable(int tableid) throws DatabaseException {

    return this.em.find(MTable.class, tableid);
  }

  public Field getField(int fieldid) throws DatabaseException {

    return this.em.find(Field.class, fieldid);
  }

  public TupleToFile getTupletofile(int tupleid) throws DatabaseException {

    String query = "TupleToFile.findByTupleid";
    Query q = this.em.createNamedQuery(query);
    q.setParameter("tupleid", tupleid);

    List<TupleToFile> result = q.getResultList();

    return result.get(0);
  }

  /**
   * Deletes a tables entity from the tables table. If the object is an
   * unmanaged entity it has to be merged to become managed so that the delete
   * can cascade down its associations if necessary
   * <p>
   *
   * @param table The table object that's going to be removed
   * @throws se.kth.meta.exception.DatabaseException
   */
  public void deleteTable(MTable table) throws DatabaseException {

    try {
      MTable t = this.getTable(table.getId());

//    FORCE DELETE NEEDS TO BE REFACTORED
//            t.setForceDelete(table.forceDelete());
//            if (!t.getFields().isEmpty() && !t.forceDelete()) {
//                throw new DatabaseException("MTable '" + t.getName() + "' has fields "
//                        + "associated to it");
//            }
      //first remove all the child elements of this table to avoid foreign key violation
      List<Field> fields = t.getFields();
      for (Field field : fields) {
        //FORCE DELETE NEEDS TO BE REFACTORED
//        field.setForceDelete(true);
        this.deleteField(field);
      }

      //now move on to remove the table
      this.utx.begin();
      if (this.em.contains(t)) {
        this.em.remove(t);
      } else {
        //if the object is unmanaged it has to be managed before it is removed
        this.em.remove(this.em.merge(t));
      }
      this.utx.commit();
    } catch (NotSupportedException | SystemException | RollbackException |
            HeuristicMixedException | HeuristicRollbackException |
            SecurityException | IllegalStateException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new DatabaseException(Dbao.class.getName(),
              "Could not delete table " + ex.getMessage());
    }
  }

  /**
   * adds a new record into 'templates' table.
   *
   * @param template The template name to be added
   * @throws se.kth.meta.exception.DatabaseException
   */
  public void addTemplate(Template template) throws DatabaseException {

    try {
      this.utx.begin();
      this.em.persist(template);
      this.utx.commit();
    } catch (IllegalStateException | SecurityException | HeuristicMixedException |
            HeuristicRollbackException | NotSupportedException |
            RollbackException |
            SystemException e) {

      throw new DatabaseException(Dbao.class.getName(),
              "Could not add template " + e.getMessage());
    }
  }

  public void removeTemplate(Template template) throws DatabaseException {
    try {
      Template t = this.em.find(Template.class, template.getId());

      //now move on to remove the table
      this.utx.begin();
      if (this.em.contains(t)) {
        this.em.remove(t);
      } else {
        //if the object is unmanaged it has to be managed before it is removed
        this.em.remove(this.em.merge(t));
      }
      this.utx.commit();
    } catch (NotSupportedException | SystemException | RollbackException |
            HeuristicMixedException | HeuristicRollbackException |
            SecurityException | IllegalStateException ex) {
      logger.log(Level.SEVERE, null, ex);
      throw new DatabaseException(Dbao.class.getName(),
              "Could not remove template " + ex.getMessage());
    }
  }

  /**
   * Deletes a fields entity from the fields table. If the object is an
   * unmanaged entity it has to be merged to become managed so that the delete
   * can cascade down its associations if necessary
   * <p>
   *
   * @param field the field object that's going to be re
   * @throws se.kth.meta.exception.DatabaseException when the field to be
   * deleted is associated to raw data
   */
  public void deleteField(Field field) throws DatabaseException {

    try {
      //FORCE DELETE NEEDS TO BE REFACTORED
      Field f = this.em.find(Field.class, field.getId());
//      f.setForceDelete(field.forceDelete());
//      if (!f.getRawData().isEmpty() && !f.forceDelete()) {
//        throw new DatabaseException("Field '" + f.getName() + "' has data "
//                + "associated to it");
//      }
      this.utx.begin();

      if (this.em.contains(f)) {
        this.em.remove(f);
      } else {
        //if the object is unmanaged it has to be managed before it is removed
        this.em.remove(this.em.merge(f));
      }
      this.utx.commit();
    } catch (SecurityException | IllegalStateException | RollbackException |
            HeuristicMixedException | HeuristicRollbackException |
            SystemException |
            NotSupportedException ex) {

      logger.log(Level.SEVERE, null, ex);
      throw new DatabaseException(Dbao.class.getName(),
              "Could not delete field " + ex.getMessage());
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
   * @throws se.kth.meta.exception.DatabaseException when an error happens
   */
  public void deleteFieldPredefinedValues(int fieldid) throws DatabaseException {

    Query query = this.em.
            createNamedQuery("FieldPredefinedValues.findByFieldid");
    query.setParameter("fieldid", fieldid);
    List<FieldPredefinedValue> valueList = query.getResultList();

    for (FieldPredefinedValue value : valueList) {
      try {

        this.utx.begin();

        if (this.em.contains(value)) {
          this.em.remove(value);
        } else {
          //if the object is unmanaged it has to be managed before it is removed
          this.em.remove(this.em.merge(value));
        }
        this.utx.commit();
      } catch (SecurityException | IllegalStateException | RollbackException |
              HeuristicMixedException | HeuristicRollbackException |
              SystemException |
              NotSupportedException ex) {

        logger.log(Level.SEVERE, null, ex);
        throw new DatabaseException(Dbao.class.getName(),
                "Could not delete predefined value " + ex.getMessage());
      }
    }
  }

  /**
   * adds a new record into 'raw_data' table. RawData is the object that's
   * going to be persisted to the database
   * <p>
   *
   * @param raw
   * @throws se.kth.meta.exception.DatabaseException
   */
  public void addRawData(RawData raw) throws DatabaseException {

    try {
      this.utx.begin();
      this.em.persist(raw);
      this.utx.commit();

    } catch (IllegalStateException | SecurityException | HeuristicMixedException |
            HeuristicRollbackException | NotSupportedException |
            RollbackException |
            SystemException e) {

      throw new DatabaseException(Dbao.class.getName(),
              "Cound not store raw data " + e.getMessage());
    }
  }

  public void addTupleToFile(TupleToFile ttf) throws DatabaseException {
    try {
      this.utx.begin();
      this.em.persist(ttf);
      this.utx.commit();

    } catch (IllegalStateException | SecurityException | HeuristicMixedException |
            HeuristicRollbackException | NotSupportedException |
            RollbackException |
            SystemException e) {

      throw new DatabaseException(Dbao.class.getName(),
              "Could not associate metadata to file " + e.getMessage());
    }
  }

  public int getLastInsertedTupleId() throws DatabaseException {

    String queryString = "RawData.lastInsertedTupleId";

    Query query = this.em.createNamedQuery(queryString);
    List<RawData> list = query.getResultList();

    return (!list.isEmpty()) ? list.get(0).getId() : 0;
  }

  public List<Template> loadTemplates() {

    String queryString = "Templates.findAll";
    Query query = this.em.createNamedQuery(queryString);
    return query.getResultList();
  }

  public List<MTable> loadTemplateContent(int templateId) {

    String queryString = "Tables.fetchTemplate";
    //this.em.clear();
    Query query = this.em.createNamedQuery(queryString);
    query.setParameter("templateid", templateId);
    return query.getResultList();
  }

  public List<FieldType> loadFieldTypes() {

    String queryString = "FieldTypes.findAll";
    Query query = this.em.createNamedQuery(queryString);

    return query.getResultList();
  }

  /**
   * Find the Template that has <i>templateid</i> as id.
   * <p>
   * @param templateid
   * @return
   */
  public Template findTemplateById(int templateid) {
    TypedQuery<Template> query = em.createNamedQuery("Templates.findByTemplateid",
            Template.class);

    query.setParameter("templateid", templateid);
    return query.getSingleResult();
  }

  /**
   * Update the relationship table <i>meta_template_to_inode</i>
   * <p>
   * @param template
   * @throws se.kth.meta.exception.DatabaseException
   */
  public void updateTemplatesInodesMxN(Template template) throws
          DatabaseException {
    try {
      this.utx.begin();
      this.em.merge(template);
      this.utx.commit();
    } catch (IllegalStateException | SecurityException | HeuristicMixedException |
            HeuristicRollbackException | NotSupportedException |
            RollbackException |
            SystemException e) {

      throw new DatabaseException(Dbao.class.getName(),
              "Problem when attaching template " + template.getId());
    }
  }

  public void shutdown() throws DatabaseException {

  }
}
