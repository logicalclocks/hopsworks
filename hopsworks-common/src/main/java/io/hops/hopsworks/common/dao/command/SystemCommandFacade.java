package io.hops.hopsworks.common.dao.command;
/*
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
 *
 */

import io.hops.hopsworks.common.dao.host.Hosts;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

@Stateless
public class SystemCommandFacade {
 
  public enum OP {
   SERVICE_KEY_ROTATION
  }
  
  public enum STATUS {
   ONGOING,
   FINISHED,
   FAILED
  }
  
  @PersistenceContext(unitName = "kthfsPU")
  private EntityManager entityManager;
  
  public SystemCommand findById(Integer id) {
    return entityManager.find(SystemCommand.class, id);
  }
  
  public List<SystemCommand> findAll() {
    TypedQuery<SystemCommand> query = entityManager.createNamedQuery("SystemCommand.findAll", SystemCommand.class);
    return query.getResultList();
  }
  
  public List<SystemCommand> findByHost(Hosts host) {
    TypedQuery<SystemCommand> query = entityManager.createNamedQuery("SystemCommand.findByHost", SystemCommand.class);
    query.setParameter("host", host);
    return query.getResultList();
  }
  
  public void persist(SystemCommand command) {
    entityManager.persist(command);
  }
  
  public void update(SystemCommand command) {
    entityManager.merge(command);
  }
  
  public void delete(SystemCommand command) {
    entityManager.remove(command);
  }
}
