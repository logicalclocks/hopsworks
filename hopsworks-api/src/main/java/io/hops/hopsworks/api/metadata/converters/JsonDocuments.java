/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.api.metadata.converters;

/*
 * Represents a "documents" database column that holds json document data
 */
public class JsonDocuments {

  private String docId;
  private String docName;

  public String getDocId() {
    return docId;
  }

  public void setDocId(String docId) {
    this.docId = docId;
  }

  public String getDocName() {
    return docName;
  }

  public void setDocName(String docName) {
    this.docName = docName;
  }

  public JsonDocuments() {
    super();
  }

  public JsonDocuments(String docId, String docName) {
    super();
    this.docId = docId;
    this.docName = docName;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || obj instanceof JsonDocuments == false) {
      return false;
    }
    JsonDocuments other = (JsonDocuments) obj;
    if (this.docId.compareTo(other.docId) == 0 && this.docName.compareTo(
            other.docName) == 0) {
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return "JsonDocuments [docId=" + docId + ", docName=" + docName + "]";
  }

}
