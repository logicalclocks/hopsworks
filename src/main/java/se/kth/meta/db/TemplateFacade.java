package se.kth.meta.db;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import se.kth.kthfsdashboard.user.AbstractFacade;
import se.kth.meta.entity.MTable;
import se.kth.meta.entity.Template;
import se.kth.meta.exception.DatabaseException;

/**
 *
 * @author vangelis
 */
@Stateless
public class TemplateFacade extends AbstractFacade<Template> {

  private static final Logger logger = Logger.getLogger(TemplateFacade.class.
          getName());

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public TemplateFacade() {
    super(Template.class);
  }

  public Template getTemplate(int templateId) {
    return this.em.find(Template.class, templateId);
  }

  /**
   * adds a new record into 'templates' table.
   *
   * @param template The template name to be added
   * @return
   * @throws se.kth.meta.exception.DatabaseException
   */
  public int addTemplate(Template template) throws DatabaseException {

    try {
      Template t = this.getTemplate(template.getId());

      if (t != null && t.getId() != -1) {

        t.copy(template);
        this.em.merge(t);
      } else {

        t = template;
        t.getMTables().clear();
        this.em.persist(t);
      }

      this.em.flush();
      this.em.clear();
      return t.getId();
    } catch (IllegalStateException | SecurityException e) {

      throw new DatabaseException(MTableFacade.class.getName(), e.getMessage());
    }
  }

  public void removeTemplate(Template template) throws DatabaseException {
    try {
      Template t = this.getTemplate(template.getId());

      if (this.em.contains(t)) {
        this.em.remove(t);
      } else {
        //if the object is unmanaged it has to be managed before it is removed
        this.em.remove(this.em.merge(t));
      }

    } catch (SecurityException | IllegalStateException ex) {
      throw new DatabaseException(TemplateFacade.class.getName(),
              "Could not remove template " + ex.getMessage());
    }
  }

  public List<Template> loadTemplates() {

    String queryString = "Template.findAll";
    Query query = this.em.createNamedQuery(queryString);
    return query.getResultList();
  }

  public List<MTable> loadTemplateContent(int templateId) {

    String queryString = "MTable.findByTemplateId";

    Query query = this.em.createNamedQuery(queryString);
    query.setParameter("templateid", templateId);

    List<MTable> modifiedEntities = query.getResultList();

    //force em to fetch the changed entities from the database
    for (MTable table : modifiedEntities) {
      this.em.refresh(table);
    }

    Collections.sort(modifiedEntities);
    return modifiedEntities;
  }

  /**
   * Find the Template that has <i>templateid</i> as id.
   * <p>
   * @param templateid
   * @return Null if no such template was found.
   */
  public Template findByTemplateId(int templateid) {
    TypedQuery<Template> query = em.
            createNamedQuery("Template.findById",
                    Template.class);

    query.setParameter("templateid", templateid);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      //There is no such id.
      return null;
    }
  }

  /**
   * Update the relationship table <i>meta_template_to_inode</i>
   * <p>
   * @param template
   * @throws se.kth.meta.exception.DatabaseException
   */
  public void updateTemplatesInodesMxN(Template template) throws
          DatabaseException {

    this.em.merge(template);
  }

}
