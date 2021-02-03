/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.admin.featurestore.tag;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagSchemaControllerIface;
import io.hops.hopsworks.restutils.RESTException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import javax.inject.Inject;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ManagedBean
@RequestScoped
public class CreateTagsBean {
  private static final Logger LOGGER = Logger.getLogger(CreateTagsBean.class.getName());
  
  private String name;
  private String schema;
  
  private List<Tag> tags;
  
  @Inject
  private FeatureStoreTagSchemaControllerIface featureStoreTagSchemaController;
  
  @PostConstruct
  public void init() {
    tags = getAllTagsAsList();
  }
  
  private List<Tag> getAllTagsAsList() {
    return featureStoreTagSchemaController.getAll().entrySet().stream()
      .map(tag -> new Tag(tag.getKey(), tag.getValue())).collect(Collectors.toList());
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getSchema() {
    return schema;
  }
  
  public void setSchema(String schema) {
    this.schema = schema;
  }
  
  public List<Tag> getTags() {
    return tags;
  }
  
  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }
  
  public void save() {
    try {
      featureStoreTagSchemaController.create(name.trim(), schema);
      MessagesController.addInfoMessage("Added new tag: ", name);
    } catch(RESTException re) {
      String detail = getRestMsg(re);
      MessagesController.addErrorMessage("Failed to save tag", detail);
      LOGGER.log(Level.FINE, detail);
    } catch (Exception e) {
      String detail = getCause(e);
      MessagesController.addErrorMessage("Failed to save tag", detail);
      LOGGER.log(Level.FINE, detail);
    } finally {
      this.tags = getAllTagsAsList();
      this.name = null;
      this.schema = null;
    }
  }
  
  public void delete(String tagName) {
    if (tagName == null) {
      return;
    }
    try {
      featureStoreTagSchemaController.delete(tagName);
      MessagesController.addInfoMessage("Deleted tag: ", tagName);
    } catch (Exception e) {
      String detail = getCause(e);
      MessagesController.addErrorMessage("Failed to delete tag", detail);
      LOGGER.log(Level.FINE, detail);
    } finally {
      this.tags = getAllTagsAsList();
    }
  }
  
  public void noSpaceValidator(FacesContext ctx, UIComponent uc, Object object) throws ValidatorException {
    String input;
    if (object != null && object instanceof String) {
      input = (String) object;
      if (input.trim().contains(" ")) {
        ((UIInput) uc).setValid(false);
        FacesMessage msgError = new FacesMessage();
        msgError.setSeverity(FacesMessage.SEVERITY_ERROR);
        msgError.setSummary("Validation error:");
        msgError.setDetail("Tag can not contain space.");
        throw new ValidatorException(msgError);
      }
    } else {
      ((UIInput) uc).setValid(false);
      FacesMessage msgError = new FacesMessage();
      msgError.setSeverity(FacesMessage.SEVERITY_ERROR);
      msgError.setSummary("Validation error:");
      msgError.setDetail("Tag can not be empty.");
      throw new ValidatorException(msgError);
    }
  }
  
  private String getCause(Exception e) {
    Throwable cause = e.getCause();
    if (cause == null) {
      return e.getMessage();
    }
    while(cause.getCause() != null) {
      cause = cause.getCause();
    }
    return cause.getMessage();
  }
  
  private String getRestMsg(RESTException re) {
    return re.getUsrMsg() != null? re.getUsrMsg() : re.getErrorCode().getMessage();
  }
}
