package se.kth.hopsworks.zeppelin.rest;

import javax.xml.bind.annotation.XmlRootElement;
import org.apache.zeppelin.interpreter.InterpreterSetting;

@XmlRootElement
public class InterpreterDTO {
  
  private String id;
  private String name;
  private String group;
  private boolean notRunning;

  public InterpreterDTO() {
  }

  public InterpreterDTO(String id, String name, String group, boolean notRunning) {
    this.id = id;
    this.name = name;
    this.group = group;
    this.notRunning = notRunning;
  }
  
  public InterpreterDTO(InterpreterSetting interpreter, boolean notRunning) {
    this.id = interpreter.id();
    this.name = interpreter.getName();
    this.group = interpreter.getGroup();
    this.notRunning = notRunning;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public boolean isNotRunning() {
    return notRunning;
  }

  public void setNotRunning(boolean notRunning) {
    this.notRunning = notRunning;
  }
}