package io.hops.hopsworks.api.zeppelin.rest;

import io.hops.hopsworks.api.zeppelin.util.LivyMsg;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.zeppelin.interpreter.InterpreterSetting;

@XmlRootElement
public class InterpreterDTO {

  private String id;
  private String name;
  private String group;
  private List<LivyMsg.Session> sessions;
  private boolean notRunning;

  public InterpreterDTO() {
  }

  public InterpreterDTO(String id, String name, String group, boolean notRunning) {
    this.id = id;
    this.name = name;
    this.group = group;
    this.notRunning = notRunning;
    sessions = new ArrayList<>();
  }

  public InterpreterDTO(InterpreterSetting interpreter, boolean notRunning) {
    this.id = interpreter.getId();
    this.name = interpreter.getName();
    this.group = null;
    this.notRunning = notRunning;
    sessions = new ArrayList<>();
  }

  public InterpreterDTO(InterpreterSetting interpreter, boolean notRunning,
          List<LivyMsg.Session> runningLivySessions) {
    this.id = interpreter.getId();
    this.name = interpreter.getName();
    this.group = null;
    this.notRunning = notRunning;
    sessions = runningLivySessions;
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

  public List<LivyMsg.Session> getSessions() {
    return sessions;
  }

  public void setSessions(List<LivyMsg.Session> sessions) {
    this.sessions = sessions;
  }

  public boolean isNotRunning() {
    return notRunning;
  }

  public void setNotRunning(boolean notRunning) {
    this.notRunning = notRunning;
  }
}
