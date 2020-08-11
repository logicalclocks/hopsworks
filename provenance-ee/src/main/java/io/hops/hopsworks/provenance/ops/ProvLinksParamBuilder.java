/*
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
 */
package io.hops.hopsworks.provenance.ops;

import io.hops.hopsworks.common.provenance.core.ProvParser;
import io.hops.hopsworks.exceptions.ProvenanceException;
import io.hops.hopsworks.restutils.RESTCodes;
import org.javatuples.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class ProvLinksParamBuilder {
  boolean appIdDefined = false;
  boolean inArtifactDefined = false;
  boolean outArtifactDefined = false;
  Map<ProvParser.Field, ProvParser.FilterVal> inFilterBy = new HashMap<>();
  Map<ProvParser.Field, ProvParser.FilterVal> outFilterBy = new HashMap<>();
  Pair<Integer, Integer> pagination = null;
  boolean fullLink = true;
  
  public ProvLinksParamBuilder filterByFields(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<ProvLinksParser.Field, Object> filter = ProvLinksParser.extractFilter(param);
      filterBy(filter.getValue0(), filter.getValue1());
    }
    return this;
  }
  
  public void outFilterByField(ProvLinksParser.FieldsPF field, Object val) throws ProvenanceException {
    switch(field) {
      case APP_ID: {
        appIdDefined = true;
        ProvLinksParser.filterBySanityCheck(outFilterBy, field);
        ProvParser.addToFilters(outFilterBy, field, val);
      } break;
      case IN_ARTIFACT:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          "cannot modify IN_ARTIFACT in refine out query");
      case OUT_ARTIFACT: {
        outArtifactDefined = true;
        ProvLinksParser.filterBySanityCheck(outFilterBy, field);
        ProvParser.addToFilters(outFilterBy, field, val);
      } break;
      default: break;
    }
  }
  
  public void inFilterByField(ProvLinksParser.FieldsPF field, Object val) throws ProvenanceException {
    switch(field) {
      case APP_ID: {
        appIdDefined = true;
        ProvLinksParser.filterBySanityCheck(inFilterBy, field);
        ProvParser.addToFilters(inFilterBy, field, val);
      } break;
      case IN_ARTIFACT: {
        inArtifactDefined = true;
        ProvLinksParser.filterBySanityCheck(inFilterBy, field);
        ProvParser.addToFilters(inFilterBy, field, val);
      } break;
      case OUT_ARTIFACT:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          "cannot modify OUT_ARTIFACT in refine in query");
      default: {
        ProvLinksParser.filterBySanityCheck(inFilterBy, field);
        ProvParser.addToFilters(inFilterBy, field, val);
      }
    }
  }
  
  private void filterBy(ProvLinksParser.Field field, Object val) throws ProvenanceException {
    if(field instanceof ProvLinksParser.FieldsPF) {
      ProvLinksParser.FieldsPF fieldPF = (ProvLinksParser.FieldsPF) field;
      switch (fieldPF) {
        case APP_ID: {
          appIdDefined = true;
          ProvLinksParser.filterBySanityCheck(inFilterBy, field);
          ProvParser.addToFilters(inFilterBy, field, val);
          ProvLinksParser.filterBySanityCheck(outFilterBy, field);
          ProvParser.addToFilters(outFilterBy, field, val);
        }
        break;
        case IN_ARTIFACT: {
          inArtifactDefined = true;
          ProvLinksParser.filterBySanityCheck(inFilterBy, field);
          ProvParser.addToFilters(inFilterBy, field, val);
        }
        break;
        case IN_TYPE: {
          ProvLinksParser.filterBySanityCheck(inFilterBy, field);
          ProvParser.addToFilters(inFilterBy, field, val);
        }
        break;
        case OUT_ARTIFACT: {
          outArtifactDefined = true;
          ProvLinksParser.filterBySanityCheck(outFilterBy, field);
          ProvParser.addToFilters(outFilterBy, field, val);
        }
        break;
        case OUT_TYPE: {
          ProvLinksParser.filterBySanityCheck(outFilterBy, field);
          ProvParser.addToFilters(outFilterBy, field, val);
        }
        break;
        default: {
          ProvLinksParser.filterBySanityCheck(inFilterBy, field);
          ProvParser.addToFilters(inFilterBy, field, val);
          ProvLinksParser.filterBySanityCheck(outFilterBy, field);
          ProvParser.addToFilters(outFilterBy, field, val);
        }
      }
    }
  }
  
  public ProvLinksParamBuilder onlyApps(boolean enabled) throws ProvenanceException {
    if(enabled) {
      onlyApps();
    }
    return this;
  }
  
  public ProvLinksParamBuilder onlyApps() throws ProvenanceException {
    ProvParser.addToFilters(inFilterBy, ProvLinksParser.FieldsPF.ONLY_APPS, "none");
    ProvParser.addToFilters(outFilterBy, ProvLinksParser.FieldsPF.ONLY_APPS, "none");
    return this;
  }
  
  public ProvLinksParamBuilder paginate(Integer offset, Integer limit) {
    pagination = Pair.with(offset, limit);
    return this;
  }
  
  public ProvLinksParamBuilder linkType(boolean fullLink) {
    this.fullLink = fullLink;
    return this;
  }
}
