package se.kth.bbc.jobs.cuneiform.model;

import com.google.common.base.Strings;
import de.huberlin.wbi.cuneiform.core.semanticmodel.HasFailedException;
import de.huberlin.wbi.cuneiform.core.semanticmodel.TopLevelContext;
import de.huberlin.wbi.cuneiform.core.staticreduction.StaticNodeVisitor;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import se.kth.bbc.jobs.DatabaseJsonObject;
import se.kth.bbc.jobs.JsonReducable;
import se.kth.bbc.lims.Utils;

/**
 * Represents a (Cuneiform) workflow. It has a name, the contents of the
 * workflow file, a list of input parameters and a list of output parameters.
 * <p>
 * @author stig
 */
@XmlRootElement
public class WorkflowDTO implements JsonReducable {

  private String name;
  private String contents;
  private String path;
  private List<InputParameter> inputParams;
  private List<OutputParameter> outputParams;

  protected static final String KEY_PATH = "path";
  protected static final String KEY_INPUT = "input";
  protected static final String KEY_OUTPUT = "output";
  protected static final String KEY_NAME = "name";

  /**
   * No-arg constructor for JAXB.
   */
  public WorkflowDTO() {
  }

  /**
   * Creates a new WorkflowDTO with the given name.
   * <p>
   * @param path
   */
  public WorkflowDTO(String path) {
    this(path, null);
  }

  /**
   * Creates a new WorkflowDTO with the given name, contents.
   * <p>
   * @param path
   * @param contents
   */
  public WorkflowDTO(String path, String contents) {
    this.name = Utils.getFileName(path);
    this.contents = contents;
    this.path = path;
  }

  /**
   * Get the name of the workflow.
   * <p>
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * Set the name of the workflow.
   * <p>
   * @param name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Get the contents of the workflow file.
   * <p>
   * @return
   */
  public String getContents() {
    return contents;
  }

  /**
   * Set the contents of the workflow file.
   * This operation resets the contents of the input and output parameter lists.
   * <p>
   * @param contents
   */
  public void setContents(String contents) {
    this.contents = contents;
    this.inputParams = new ArrayList<>();
    this.outputParams = new ArrayList<>();
  }

  /**
   * Get the list of input parameters. This WorkflowDTO is backed by this list.
   * <p>
   * @return
   */
  public List<InputParameter> getInputParams() {
    return inputParams;
  }

  /**
   * Set the list of input parameters. This workflowDTO is then backed by this
   * list.
   * <p>
   * @param inputParams
   */
  public void setInputParams(List<InputParameter> inputParams) {
    this.inputParams = inputParams;
  }

  /**
   * Get the list of output parameters. This WorkflowDTO is backed by the
   * returned list.
   * <p>
   * @return
   */
  public List<OutputParameter> getOutputParams() {
    return outputParams;
  }

  /**
   * Set the list of output parameters. This WorkflowDTO is then backed by this
   * list.
   * <p>
   * @param outputParams
   */
  public void setOutputParams(List<OutputParameter> outputParams) {
    this.outputParams = outputParams;
  }

  /**
   * Get the input parameter with the given name. Returns null if not found.
   * <p>
   * @param name
   * @return The requested input parameter or null if not found.
   */
  public InputParameter getInputParameter(String name) {
    for (InputParameter ip : inputParams) {
      if (ip.getName().equals(name)) {
        return ip;
      }
    }
    return null;
  }

  /**
   * Get the output parameter with the given name. Returns null if not found.
   * <p>
   * @param name
   * @return The requested output parameter or null if not found.
   */
  public OutputParameter getOutputParameter(String name) {
    for (OutputParameter op : outputParams) {
      if (op.getName().equals(name)) {
        return op;
      }
    }
    return null;
  }

  /**
   * Set the value of the input parameter with the given name. Returns false if
   * no inputparameter with such name was found.
   * <p>
   * @param name
   * @param value
   * @return
   */
  public boolean setInputValue(String name, String value) {
    for (InputParameter ip : inputParams) {
      if (ip.getName().equals(name)) {
        ip.setValue(value);
        return true;
      }
    }
    return false;
  }

  /**
   * Set the queried status of the output parameter with the given name. Returns
   * false if no outputparameter with such name was found.
   * <p>
   * @param name
   * @param queried
   * @return
   */
  public boolean setOutputQueried(String name, boolean queried) {
    for (OutputParameter op : outputParams) {
      if (op.getName().equals(name)) {
        op.setQueried(queried);
        return true;
      }
    }
    return false;
  }

  public String getPath() {
    return path;
  }

  /**
   * Set the path where the base workflow, before content alterations, resides.
   * <p>
   * @param path
   */
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Inspect the workflow. This method analyzes the contents of the workflow and
   * sets the inputParams and outputParams fields of the object. It overwrites
   * all previously set input and output parameters.
   * <p>
   * @throws HasFailedException
   * @throws IllegalStateException If the contents had not yet been set.
   */
  public void inspect() throws HasFailedException, IllegalStateException {
    if (Strings.isNullOrEmpty(contents)) {
      throw new IllegalStateException("Cannot inspect empty workflow contents.");
    }
    // Inspect the workflow and get the parameter lists
    TopLevelContext tlc = StaticNodeVisitor.createTlc(contents);
    List<String> freenames = StaticNodeVisitor.getFreeVarNameList(tlc);
    List<String> outnames = StaticNodeVisitor.getTargetVarNameList(tlc);
    // Construct and fill the parameter lists
    inputParams = new ArrayList<>(freenames.size());
    outputParams = new ArrayList<>(outnames.size());
    for (String par : freenames) {
      inputParams.add(new InputParameter(par));
    }
    for (String par : outnames) {
      outputParams.add(new OutputParameter(par));
    }
  }

  /**
   * Update the workflow contents from the parameter bindings and query status.
   * Parameter bindings are literally put into the file. Hence, paths should be
   * absolute.
   * <p>
   * @throws IllegalStateException If the contents had not yet been set.
   */
  public void updateContentsFromVars() throws IllegalStateException {
    if (Strings.isNullOrEmpty(contents)) {
      throw new IllegalStateException("Cannot inspect empty workflow contents.");
    }
    StringBuilder extraLines = new StringBuilder(); //Contains the extra workflow lines
    //find out which free variables were bound (the ones that have a non-null value)
    for (InputParameter ip : inputParams) {
      if (ip.isBound()) {
        //add a line to the workflow file
        String value = ip.getValue();
        while (value.startsWith("/")) {
          //need to strip starting slashes from paths...
          value = value.substring(1);
        }
        extraLines.append(ip.getName()).append(" = '").append(value).
                append("';\n");
      }
    }
    // for all selected target vars: add "<varname>;" to file
    for (OutputParameter op : outputParams) {
      if (op.isQueried()) {
        extraLines.append(op.getName()).append(";\n");
      }
    }
    contents += "\n" + extraLines.toString();
  }

  @Override
  public DatabaseJsonObject getReducedJsonObject() {
    DatabaseJsonObject obj = new DatabaseJsonObject();
    obj.set(KEY_PATH, path);
    obj.set(KEY_NAME, name);
    //Construct input object and add to object
    DatabaseJsonObject inputs = new DatabaseJsonObject();
    for (InputParameter ip : inputParams) {
      inputs.set(ip.getName(), ip.getValue());
    }
    obj.set(KEY_INPUT, inputs);
    //Construct output object
    DatabaseJsonObject outputs = new DatabaseJsonObject();
    for (OutputParameter op : outputParams) {
      outputs.set(op.getName(), op.isQueried() ? "true" : "false");
    }
    obj.set(KEY_OUTPUT, outputs);
    return obj;
  }

  @Override
  public void updateFromJson(DatabaseJsonObject json) throws
          IllegalArgumentException {
    //First: make sure the given object is valid by getting the type and AdamCommandDTO
    String jsonPath, jsonName;
    List<InputParameter> inputs;
    List<OutputParameter> outputs;
    try {
      jsonPath = json.getString(KEY_PATH);
      jsonName = json.getString(KEY_NAME);
      DatabaseJsonObject jsonInputs = json.getJsonObject(KEY_INPUT);
      inputs = new ArrayList<>(jsonInputs.size());
      for(String s : jsonInputs.keySet()){
        inputs.add(new InputParameter(s, jsonInputs.getString(s)));
      }
      DatabaseJsonObject jsonOutputs = json.getJsonObject(KEY_OUTPUT);
      outputs = new ArrayList<>(jsonOutputs.size());
      for(String s : jsonOutputs.keySet()){
        outputs.add(new OutputParameter(s, "true".equals(jsonOutputs.getString(s))));
      }
    } catch (Exception e) {
      throw new IllegalArgumentException(
              "Cannot convert object into CuneiformJobConfiguration.", e);
    }
    //Second: we're now sure everything is valid: actually update the state
    this.path = jsonPath;
    this.name = jsonName;
    this.inputParams = inputs;
    this.outputParams = outputs;
  }

}
