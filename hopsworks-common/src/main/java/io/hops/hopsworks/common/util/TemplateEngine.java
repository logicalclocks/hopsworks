/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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
 */

package io.hops.hopsworks.common.util;

import freemarker.ext.util.WrapperTemplateModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Singleton
@TransactionAttribute(TransactionAttributeType.NEVER)
public class TemplateEngine {
  private final static Logger LOGGER = Logger.getLogger(TemplateEngine.class.getName());
  
  @EJB
  private Settings settings;
  
  private Configuration configuration;
  
  @PostConstruct
  public void init() {
    Path templatesDirectory = Paths.get(settings.getHopsworksDomainDir(), "templates");
    configuration = new Configuration(Configuration.VERSION_2_3_29);
    try {
      configuration.setDirectoryForTemplateLoading(templatesDirectory.toFile());
      configuration.setDefaultEncoding("UTF-8");
      configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
      configuration.setLogTemplateExceptions(false);
      configuration.setWrapUncheckedExceptions(true);
      configuration.setFallbackOnNullLoopVariable(false);
    } catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Failed to configure Templating engine!", ex);
    }
  }
  
  @Lock(LockType.READ)
  public void template(String templateName, Map<String, Object> dataModel, Writer out)
      throws IOException, TemplateException {
    isInitialized();
    if (dataModel == null) {
      throw new IOException("Data model is null");
    }
    dataModel.putIfAbsent("instanceOf", new InstanceOf());
    Template template = configuration.getTemplate(templateName);
    template.process(dataModel, out);
  }
  
  private void isInitialized() throws IOException {
    if (configuration == null) {
      throw new IOException("Template engine is not initialized");
    }
  }
  
  private class InstanceOf implements TemplateMethodModelEx {
  
    @Override
    public Object exec(List list) throws TemplateModelException {
      if (list.size() != 2) {
        throw new TemplateModelException("Wrong usage. First argument is an object and the second is a class");
      }
      Object obj = ((WrapperTemplateModel)list.get(0)).getWrappedObject();
      Object cls = ((WrapperTemplateModel)list.get(1)).getWrappedObject();
      if (!(cls instanceof Class)) {
        throw new TemplateModelException("Second argument must be a Class");
      }
      return ((Class)cls).isAssignableFrom(obj.getClass());
    }
  }
}
