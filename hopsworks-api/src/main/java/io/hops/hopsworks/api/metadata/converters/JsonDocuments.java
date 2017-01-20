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
