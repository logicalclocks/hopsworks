/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 */

package io.hops.hopsworks.common.dao.metadata.db;

import io.hops.hopsworks.common.dao.AbstractFacade;
import io.hops.hopsworks.common.dao.metadata.MTable;
import io.hops.hopsworks.common.dao.metadata.Template;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
public class TemplateFacade extends AbstractFacade<Template> {

  private static final Logger LOGGER = Logger.getLogger(TemplateFacade.class.getName());

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
   */
  public int addTemplate(Template template) {

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
    } catch (IllegalStateException | SecurityException ex) {
      LOGGER.log(Level.SEVERE, "Could not add template " + template, ex);
      throw ex;
    }
  }

  public void removeTemplate(Template template) {
    try {
      Template t = this.getTemplate(template.getId());

      if (t != null) {
        if (this.em.contains(t)) {
          this.em.remove(t);
        } else {
          //if the object is unmanaged it has to be managed before it is removed
          this.em.remove(this.em.merge(t));
        }
      }
    } catch (SecurityException | IllegalStateException ex) {
      LOGGER.log(Level.SEVERE, "Could not remove template " + template, ex);
      throw ex;
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
   * <p/>
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
   * Find the Template that has <i>templateName</i> as input.
   * <p/>
   * @param templateName
   * @return Null if no such template was found.
   */
  public List<Template> findByTemplateName(String templateName) {
    TypedQuery<Template> query = em.
            createNamedQuery("Template.findByName",
                    Template.class);

    query.setParameter("name", templateName);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      //There is no such name.
      return null;
    }
  }

  /**
   * Check whether template already exists by  <i>templateName</i> as input.
   * <p/>
   * @param templateName
   * @return true if exists, false otherwise.
   */
  public boolean isTemplateAvailable(String templateName) {
    List<Template> t = findByTemplateName(templateName);
    return (t != null) && (t.size() > 0);
  }

  /**
   * Update the relationship table <i>meta_template_to_inode</i>
   * <p/>
   * @param template
   */
  public void updateTemplatesInodesMxN(Template template) {
    this.em.merge(template);
  }

}
