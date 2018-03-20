/*
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.dao.project.pia;

import io.hops.hopsworks.common.dao.AbstractFacade;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

@Stateless
public class PiaFacade extends AbstractFacade<Pia> {

  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager em;

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  public PiaFacade() {
    super(Pia.class);
  }

  public Pia findByProject(int projectId) {
    TypedQuery<Pia> q = em.createNamedQuery("Pia.findByProjectId", Pia.class);
    q.setParameter("projectId", projectId);
    List<Pia> allPias = q.getResultList();
    if (allPias == null || allPias.isEmpty()) {
      Pia p = new Pia();
      p.setProjectId(projectId);
      allPias = new ArrayList<>();
      allPias.add(p);
    }
    return allPias.iterator().next();
  }  
  
//  public Pia findByProject(Project project) {
//    Collection<Pia> allPias = project.getPiaCollection();
//    if (allPias == null || allPias.isEmpty()) {
//      Pia p = new Pia();
//      p.setProjectId(project);
//      allPias = new ArrayList<>();
//      allPias.add(p);
//    }
//    return allPias.iterator().next();
//  }
  public Pia mergeUpdate(Pia pia, int projectId) {
    Pia piaManaged = findByProject(projectId);
    pia.setProjectId(projectId);
    piaManaged.deepCopy(pia);
    return update(piaManaged);
  }
}
