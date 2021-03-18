/*
 * This file is part of Hopsworks
 * Copyright (C) 2020, Logical Clocks AB. All rights reserved
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
package io.hops.hopsworks.common.provenance.ops;

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
      Pair<ProvLinks.Field, Object> filter = ProvLinks.extractFilter(param);
      filterBy(filter.getValue0(), filter.getValue1());
    }
    return this;
  }
  
  public void outFilterByField(ProvLinks.FieldsPF field, Object val) throws ProvenanceException {
    switch(field) {
      case APP_ID: {
        appIdDefined = true;
        ProvLinks.filterBySanityCheck(outFilterBy, field);
        ProvParser.addToFilters(outFilterBy, field, val);
      } break;
      case IN_ARTIFACT:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          "cannot modify IN_ARTIFACT in refine out query");
      case OUT_ARTIFACT: {
        outArtifactDefined = true;
        ProvLinks.filterBySanityCheck(outFilterBy, field);
        ProvParser.addToFilters(outFilterBy, field, val);
      } break;
      default: break;
    }
  }
  
  public void inFilterByField(ProvLinks.FieldsPF field, Object val) throws ProvenanceException {
    switch(field) {
      case APP_ID: {
        appIdDefined = true;
        ProvLinks.filterBySanityCheck(inFilterBy, field);
        ProvParser.addToFilters(inFilterBy, field, val);
      } break;
      case IN_ARTIFACT: {
        inArtifactDefined = true;
        ProvLinks.filterBySanityCheck(inFilterBy, field);
        ProvParser.addToFilters(inFilterBy, field, val);
      } break;
      case OUT_ARTIFACT:
        throw new ProvenanceException(RESTCodes.ProvenanceErrorCode.BAD_REQUEST, Level.INFO,
          "cannot modify OUT_ARTIFACT in refine in query");
      default: {
        ProvLinks.filterBySanityCheck(inFilterBy, field);
        ProvParser.addToFilters(inFilterBy, field, val);
      }
    }
  }
  
  private void filterBy(ProvLinks.Field field, Object val) throws ProvenanceException {
    if(field instanceof ProvLinks.FieldsPF) {
      ProvLinks.FieldsPF fieldPF = (ProvLinks.FieldsPF) field;
      switch (fieldPF) {
        case APP_ID: {
          appIdDefined = true;
          ProvLinks.filterBySanityCheck(inFilterBy, field);
          ProvParser.addToFilters(inFilterBy, field, val);
          ProvLinks.filterBySanityCheck(outFilterBy, field);
          ProvParser.addToFilters(outFilterBy, field, val);
        }
        break;
        case IN_ARTIFACT: {
          inArtifactDefined = true;
          ProvLinks.filterBySanityCheck(inFilterBy, field);
          ProvParser.addToFilters(inFilterBy, field, val);
        }
        break;
        case IN_TYPE: {
          ProvLinks.filterBySanityCheck(inFilterBy, field);
          ProvParser.addToFilters(inFilterBy, field, val);
        }
        break;
        case OUT_ARTIFACT: {
          outArtifactDefined = true;
          ProvLinks.filterBySanityCheck(outFilterBy, field);
          ProvParser.addToFilters(outFilterBy, field, val);
        }
        break;
        case OUT_TYPE: {
          ProvLinks.filterBySanityCheck(outFilterBy, field);
          ProvParser.addToFilters(outFilterBy, field, val);
        }
        break;
        default: {
          ProvLinks.filterBySanityCheck(inFilterBy, field);
          ProvParser.addToFilters(inFilterBy, field, val);
          ProvLinks.filterBySanityCheck(outFilterBy, field);
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
    ProvParser.addToFilters(inFilterBy, ProvLinks.FieldsPF.ONLY_APPS, "none");
    ProvParser.addToFilters(outFilterBy, ProvLinks.FieldsPF.ONLY_APPS, "none");
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
  
  public boolean isAppIdDefined() {
    return appIdDefined;
  }
  
  public boolean isInArtifactDefined() {
    return inArtifactDefined;
  }
  
  public boolean isOutArtifactDefined() {
    return outArtifactDefined;
  }
  
  public Map<ProvParser.Field, ProvParser.FilterVal> getInFilterBy() {
    return inFilterBy;
  }
  
  public Map<ProvParser.Field, ProvParser.FilterVal> getOutFilterBy() {
    return outFilterBy;
  }
  
  public Pair<Integer, Integer> getPagination() {
    return pagination;
  }
  
  public boolean isFullLink() {
    return fullLink;
  }
}
