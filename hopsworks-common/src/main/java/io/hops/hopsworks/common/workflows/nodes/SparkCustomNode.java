package io.hops.hopsworks.common.workflows.nodes;

import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import io.hops.hopsworks.common.hdfs.DistributedFileSystemOps;
import io.hops.hopsworks.common.dao.workflow.OozieFacade;
import io.hops.hopsworks.common.dao.workflow.Node;

import javax.persistence.*;
import javax.ws.rs.ProcessingException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

@Entity
@XmlRootElement
public class SparkCustomNode extends Node {

  public SparkCustomNode() {
    this.setId(UUID.randomUUID().toString());
    this.setType("spark-custom-node");
    this.setData(new ObjectMapper().createObjectNode());
  }

  public SparkCustomNode(Element elem) {
    this.setType("spark-custom-node");
    this.setData(new ObjectMapper().createObjectNode());
    this.setId(elem.getAttribute("name"));

    this.setMainClass(elem.getElementsByTagName("class").item(0).
            getTextContent());
    this.setJar(elem.getElementsByTagName("jar").item(0).getTextContent());
    if (elem.getElementsByTagName("spark-opts").getLength() > 0) {
      this.setOpts(elem.getElementsByTagName("spark-opts").item(0).
              getTextContent());
    }

    NodeList list;

    list = elem.getElementsByTagName("arg");
    for (int i = 0; i < list.getLength(); i++) {
      this.addArgument(list.item(i).getTextContent());
    }
    list = elem.getElementsByTagName("job-xml");
    for (int i = 0; i < list.getLength(); i++) {
      this.addJobXmls(list.item(i).getTextContent());
    }

    list = elem.getElementsByTagName("delete");
    for (int i = 0; i < list.getLength(); i++) {
      this.addRmDirs(((Element) list.item(i)).getAttribute("path"));
    }

    list = elem.getElementsByTagName("mkdir");
    for (int i = 0; i < list.getLength(); i++) {
      this.addMkDirs(((Element) list.item(i)).getAttribute("path"));
    }

    list = elem.getElementsByTagName("property");
    for (int i = 0; i < list.getLength(); i++) {
      this.addConfigurations(list.item(i).getFirstChild().getTextContent(),
              list.item(i).getLastChild().getTextContent());
    }

    this.setId(elem.getAttribute("name"));
  }

  @XmlElement(name = "jar")
  public String getJar() {
    if (this.getData().get("jar") == null) {
      return null;
    }
    return this.getData().get("jar").getValueAsText();
  }

  public void setJar(String jar) {
    ObjectNode data = (ObjectNode) this.getData();
    data.put("jar", jar.replace("${nameNode}", ""));
  }

  @XmlElement(name = "mainClass")
  public String getMainClass() {
    if (this.getData().get("mainClass") == null) {
      return null;
    }
    if (this.getData().get("mainClass").isArray()) {
      return this.getData().get("mainClass").get(0).getValueAsText();
    }
    return this.getData().get("mainClass").getValueAsText();
  }

  public void setMainClass(String mainClass) {
    ObjectNode data = (ObjectNode) this.getData();
    data.put("mainClass", mainClass);
  }

  @XmlElement(name = "name")
  public String getName() {
    if (this.getData().get("name") == null) {
      return this.getId();
    }
    if (this.getData().get("name").isArray()) {
      return this.getData().get("name").get(0).getValueAsText();
    }
    return this.getData().get("name").getValueAsText();
  }

  @JsonIgnore
  @XmlTransient
  public Iterator<JsonNode> getArguments() {
    if (this.getData().get("arguments") == null) {
      return null;
    }
    if (!this.getData().get("arguments").isArray()) {
      return null;
    }
    return this.getData().get("arguments").getElements();
  }

  public void addArgument(String arg) {
    ArrayNode arguments;
    if (this.getData().get("arguments") == null) {
      ObjectNode data = (ObjectNode) this.getData();
      arguments = data.putArray("arguments");
    } else {
      arguments = (ArrayNode) this.getData().get("arguments");
    }
    arguments.add(arg);
  }

  @JsonIgnore
  @XmlTransient
  public Iterator<JsonNode> getJobXmls() {
    if (this.getData().get("jobXmls") == null) {
      return null;
    }
    if (!this.getData().get("jobXmls").isArray()) {
      return null;
    }
    return this.getData().get("jobXmls").getElements();
  }

  public void addJobXmls(String jobXml) {
    ArrayNode jobXmls;
    if (this.getData().get("jobXmls") == null) {
      ObjectNode data = (ObjectNode) this.getData();
      jobXmls = data.putArray("jobXmls");
    } else {
      jobXmls = (ArrayNode) this.getData().get("jobXmls");
    }
    jobXmls.add(jobXml);
  }

  @JsonIgnore
  @XmlTransient
  public Iterator<JsonNode> getConfigurations() {
    if (this.getData().get("configurations") == null) {
      return null;
    }
    if (!this.getData().get("configurations").isArray()) {
      return null;
    }
    return this.getData().get("configurations").getElements();
  }

  public void addConfigurations(String name, String value) {
    ArrayNode configurations, conf;
    if (this.getData().get("configurations") == null) {
      ObjectNode data = (ObjectNode) this.getData();
      configurations = data.putArray("configurations");
    } else {
      configurations = (ArrayNode) this.getData().get("configurations");
    }
    conf = configurations.arrayNode();
    conf.add(name);
    conf.add(value);
    configurations.add(conf);
  }

  @JsonIgnore
  @XmlTransient
  public Iterator<JsonNode> getMkDirs() {
    if (this.getData().get("mkDirs") == null) {
      return null;
    }
    if (!this.getData().get("mkDirs").isArray()) {
      return null;
    }
    return this.getData().get("mkDirs").getElements();
  }

  public void addMkDirs(String mkDir) {
    ArrayNode mkDirs;
    if (this.getData().get("mkDirs") == null) {
      ObjectNode data = (ObjectNode) this.getData();
      mkDirs = data.putArray("mkDirs");
    } else {
      mkDirs = (ArrayNode) this.getData().get("mkDirs");
    }
    mkDirs.add(mkDir);
  }

  @JsonIgnore
  @XmlTransient
  public Iterator<JsonNode> getRmDirs() {
    if (this.getData().get("rmDirs") == null) {
      return null;
    }
    if (!this.getData().get("rmDirs").isArray()) {
      return null;
    }
    return this.getData().get("rmDirs").getElements();
  }

  public void addRmDirs(String rmDir) {
    ArrayNode rmDirs;
    if (this.getData().get("rmDirs") == null) {
      ObjectNode data = (ObjectNode) this.getData();
      rmDirs = data.putArray("rmDirs");
    } else {
      rmDirs = (ArrayNode) this.getData().get("rmDirs");
    }
    rmDirs.add(rmDir);
  }

  @JsonIgnore
  @XmlTransient
  public String getOpts() {
    if (this.getData().get("sparkOptions") == null) {
      return null;
    }
    if (this.getData().get("sparkOptions").isArray()) {
      return this.getData().get("sparkOptions").get(0).getValueAsText();
    }

    return this.getData().get("sparkOptions").getValueAsText();
  }

  public void setOpts(String opts) {
    ObjectNode data = (ObjectNode) this.getData();
    data.put("sparkOptions", opts);
  }

  public Element getWorkflowElement(OozieFacade execution, Element root) throws
          ProcessingException {

    if ((this.getJar() != null && this.getJar().isEmpty()) || (this.
            getMainClass() != null && this.getMainClass().isEmpty())) {
      throw new ProcessingException("Missing arguments for Spark Job");
    }
    if (this.getChildren().size() != 1) {
      throw new ProcessingException("Node should only contain one descendant");
    }
    if (execution.hasNodeId(this.getOozieId())) {
      return null;
    }

    Element action = execution.getDoc().createElement("action");
    action.setAttribute("name", this.getOozieId());

    Element spark = execution.getDoc().createElement("spark");
    Node child = this.getChildren().iterator().next();
    spark.setAttribute("xmlns", "uri:oozie:spark-action:0.1");

    Element jobTracker = execution.getDoc().createElement("job-tracker");
    jobTracker.setTextContent("${jobTracker}");
    spark.appendChild(jobTracker);

    Element nameNode = execution.getDoc().createElement("name-node");
    nameNode.setTextContent("${nameNode}");
    spark.appendChild(nameNode);

    Iterator<JsonNode> rmDirs = this.getRmDirs();
    Iterator<JsonNode> mkDirs = this.getMkDirs();
    Element prepare = execution.getDoc().createElement("prepare");
    if (rmDirs != null) {
      Element del;
      while (rmDirs.hasNext()) {
        del = execution.getDoc().createElement("delete");
        del.setAttribute("path", "${nameNode}" + rmDirs.next().getValueAsText());
        prepare.appendChild(del);
      }
    }

    if (mkDirs != null) {
      Element make;
      while (mkDirs.hasNext()) {
        make = execution.getDoc().createElement("mkdir");
        make.
                setAttribute("path", "${nameNode}" + mkDirs.next().
                        getValueAsText());
        prepare.appendChild(make);
      }
    }

    spark.appendChild(prepare);

    Iterator<JsonNode> xmls = this.getJobXmls();
    if (xmls != null) {
      Element xml;
      while (xmls.hasNext()) {
        xml = execution.getDoc().createElement("job-xml");
        xml.setTextContent(xmls.next().getTextValue());
        spark.appendChild(xml);
      }
    }

    Iterator<JsonNode> confs = this.getConfigurations();
    if (confs != null) {
      Element conf, confName, confVal;
      ArrayNode arrayNode;
      while (confs.hasNext()) {
        conf = execution.getDoc().createElement("configuration");
        confName = execution.getDoc().createElement("name");
        confVal = execution.getDoc().createElement("value");
        arrayNode = (ArrayNode) confs.next();
        confName.setTextContent(arrayNode.get(0).getTextValue());
        confVal.setTextContent(arrayNode.get(1).getTextValue());
        conf.appendChild(confName);
        conf.appendChild(confVal);
        spark.appendChild(conf);
      }
    }

    Element master = execution.getDoc().createElement("master");
    master.setTextContent("${sparkMaster}");
    spark.appendChild(master);

    Element mode = execution.getDoc().createElement("mode");
    mode.setTextContent("${sparkMode}");
    spark.appendChild(mode);

    Element name = execution.getDoc().createElement("name");
    if (name != null) {
      name.setTextContent(this.getName());
      spark.appendChild(name);
    }

    Element mainClass = execution.getDoc().createElement("class");
    mainClass.setTextContent(this.getMainClass());
    spark.appendChild(mainClass);

    Element jar = execution.getDoc().createElement("jar");
    String jarPath = this.getJar();
    String jarName = jarPath.split("/")[jarPath.split("/").length - 1];
    try {
      DistributedFileSystemOps dfsops = execution.getDfs().getDfsOps();
      dfsops.copyInHdfs(new Path(jarPath), new Path(execution.getPath().concat(
              "lib/")));
    } catch (IOException e) {
      throw new ProcessingException(e.getMessage());
    }
    jar.setTextContent("${nameNode}" + execution.getPath().concat("lib/").
            concat(jarName));
    spark.appendChild(jar);

    Element opts = execution.getDoc().createElement("spark-opts");
    if (opts != null) {
      opts.setTextContent(this.getOpts());
      spark.appendChild(opts);
    }

    Iterator<JsonNode> args = this.getArguments();
    if (args != null) {
      Element arg;
      while (args.hasNext()) {
        arg = execution.getDoc().createElement("arg");
        arg.setTextContent(args.next().getTextValue());
        spark.appendChild(arg);
      }
    }

    action.appendChild(spark);

    Element ok = execution.getDoc().createElement("ok");
    ok.setAttribute("to", child.getOozieId());
    action.appendChild(ok);

    Element error = execution.getDoc().createElement("error");
    error.setAttribute("to", "kill");
    action.appendChild(error);

    root.appendChild(action);
    if (child.getClass() != JoinNode.class) {
      child.getWorkflowElement(execution, root);
    }

    execution.addNodeId(this.getOozieId());
    return action;
  }

}
