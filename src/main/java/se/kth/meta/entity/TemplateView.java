package se.kth.meta.entity;

/**
 * JSONifiable version of a Template object.
 * <p>
 * @author vangelis
 */
public class TemplateView {

  private int templateId;
  private String templateName;

  public TemplateView() {

  }

  public TemplateView(int templateId, String templateName) {
    this.templateId = templateId;
    this.templateName = templateName;
  }

  public void setTemplateId(int templateId) {
    this.templateId = templateId;
  }
  
  public int getTemplateId(){
    return this.templateId;
  }
  
  public void setTemplateName(String templateName){
    this.templateName = templateName;
  }
  
  public String getTemplateName(){
    return this.templateName;
  }
}
