/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.admin.featurestore.tag;

import io.hops.hopsworks.admin.maintenance.MessagesController;
import io.hops.hopsworks.common.featurestore.tag.FeatureStoreTagController;
import io.hops.hopsworks.persistence.entity.featurestore.tag.FeatureStoreTag;
import io.hops.hopsworks.common.dao.featurestore.tag.FeatureStoreTagFacade;
import io.hops.hopsworks.persistence.entity.featurestore.tag.TagType;
import io.hops.hopsworks.restutils.RESTException;
import org.primefaces.event.RowEditEvent;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@ManagedBean
@ViewScoped
public class CreateTagsBean {
  private static final Logger LOGGER = Logger.getLogger(CreateTagsBean.class.getName());
  
  private String name;
  private TagType type;
  private TagType[] tagTypes;
  
  private List<FeatureStoreTag> featureStoreTagList;
  
  @EJB
  private FeatureStoreTagFacade featureStoreTagFacade;
  @EJB
  private FeatureStoreTagController featureStoreTagController;
  
  @PostConstruct
  public void init() {
    tagTypes = TagType.values();
    this.featureStoreTagList = featureStoreTagFacade.findAll();
    if (this.featureStoreTagList == null) {
      this.featureStoreTagList = new ArrayList<>();
    }
  }
  
  public String getName() {
    return name;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public TagType getType() {
    return type;
  }
  
  public void setType(TagType type) {
    this.type = type;
  }
  
  public TagType[] getTagTypes() {
    return tagTypes;
  }
  
  public void setTagTypes(TagType[] tagTypes) {
    this.tagTypes = tagTypes;
  }
  
  public List<FeatureStoreTag> getFeatureStoreTagList() {
    return featureStoreTagList;
  }
  
  public void setFeatureStoreTagList(
    List<FeatureStoreTag> featureStoreTagList) {
    this.featureStoreTagList = featureStoreTagList;
  }
  
  public void save(FeatureStoreTag featureStoreTag) {
    if (featureStoreTag == null) {
      return;
    }
    try {
      if (featureStoreTag.getId() == null) {
        featureStoreTagController.create(featureStoreTag.getName(), featureStoreTag.getType());
        MessagesController.addInfoMessage("Added new tag: ", featureStoreTag.getName());
      } else {
        featureStoreTagController.update(featureStoreTag.getId(), featureStoreTag.getName(), featureStoreTag.getType());
        MessagesController.addInfoMessage("Edited tag: ", featureStoreTag.getName());
      }
    } catch(RESTException re) {
      String detail = getRestMsg(re);
      MessagesController.addErrorMessage("Failed to save tag", detail);
      LOGGER.log(Level.FINE, detail);
    } catch (Exception e) {
      String detail = getCause(e);
      MessagesController.addErrorMessage("Failed to save tag", detail);
      LOGGER.log(Level.FINE, detail);
    }
  }
  
  public void delete(FeatureStoreTag featureStoreTag) {
    if (featureStoreTag == null) {
      return;
    }
    try {
      featureStoreTagController.delete(featureStoreTag.getName());
    } catch (Exception e) {
      String detail = getCause(e);
      MessagesController.addErrorMessage("Failed to delete tag", detail);
      LOGGER.log(Level.FINE, detail);
    }
    MessagesController.addInfoMessage("Deleted tag: ", featureStoreTag.getName());
  }
  
  public void onRowEdit(RowEditEvent event) {
    FeatureStoreTag featureStoreTag = (FeatureStoreTag) event.getObject();
    save(featureStoreTag);
    this.featureStoreTagList = featureStoreTagFacade.findAll();
  }
  
  public void onRowCancel(RowEditEvent event) {
    FeatureStoreTag featureStoreTag = (FeatureStoreTag) event.getObject();
    MessagesController.addInfoMessage("Tag Edit Cancelled", featureStoreTag.getName());
  }
  
  public void onAddNew() {
    FeatureStoreTag featureStoreTag = new FeatureStoreTag(this.name.trim(), this.type);
    save(featureStoreTag);
    this.featureStoreTagList = featureStoreTagFacade.findAll();
    this.setName(null);
    this.setType(null);
  }
  
  public void remove(FeatureStoreTag featureStoreTag) {
    if (featureStoreTag != null && featureStoreTag.getId() == null) {
      this.featureStoreTagList.remove(featureStoreTag);
      return;
    }
    delete(featureStoreTag);
    this.featureStoreTagList = featureStoreTagFacade.findAll();
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
