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

import io.hops.hopsworks.exceptions.ProvenanceException;
import org.javatuples.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProvLinksParamBuilder {
  boolean appIdDefined = false;
  boolean inArtifactDefined = false;
  boolean outArtifactDefined = false;
  boolean artifactDefined = false;
  Set<String> filterBy = new HashSet<>();
  List<String> filterType = new ArrayList<>();
  Pair<Integer, Integer> pagination = null;
  boolean fullLink = true;
  Pair<Integer, Integer> expand = null;
  boolean onlyApps = true;

  public ProvLinksParamBuilder filterByFields(Set<String> params) throws ProvenanceException {
    for(String param : params) {
      Pair<ProvLinks.Field, Object> filter = ProvLinks.extractFilter(param);
      filterBy(filter.getValue0(), filter.getValue1());
    }
    return this;
  }

  private void filterBy(ProvLinks.Field field, Object val) {
    if(field instanceof ProvLinks.FieldsPF) {
      ProvLinks.FieldsPF fieldPF = (ProvLinks.FieldsPF) field;
      switch (fieldPF) {
        case APP_ID: {
          appIdDefined = true;
          expand(1,0);
          filterBy.add(val.toString());
          break;
        }
        case IN_ARTIFACT: {
          inArtifactDefined = true;
          expand(0,1);
          filterBy.add(val.toString());
          break;
        }
        case IN_TYPE:
        case OUT_TYPE:
        case ARTIFACT_TYPE: {
          filterType.addAll((List<String>)val);
          break;
        }
        case OUT_ARTIFACT: {
          outArtifactDefined = true;
          expand(1,0);
          filterBy.add(val.toString());
          break;
        }
        case ARTIFACT: {
          artifactDefined = true;
          filterBy.add(val.toString());
          break;
        }
        default: {
          filterBy.add(val.toString());
        }
      }
    }
  }

  public ProvLinksParamBuilder onlyApps(boolean onlyApps) {
    this.onlyApps = onlyApps;
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

  public ProvLinksParamBuilder expand(int expandUpstream, int expandDownstream) {
    expand = Pair.with(expandUpstream, expandDownstream);
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

  public boolean isArtifactDefined() {
    return artifactDefined;
  }

  public Set<String> getFilterBy() {
    return filterBy;
  }

  public List<String> getFilterType() {
    return filterType;
  }

  public Pair<Integer, Integer> getPagination() {
    return pagination;
  }

  public boolean isFullLink() {
    return fullLink;
  }

  public Pair<Integer, Integer> getExpand() {
    return expand;
  }

  public boolean isOnlyApps() {
    return onlyApps;
  }
}
