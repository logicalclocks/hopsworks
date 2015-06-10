package se.kth.bbc.jobs.cuneiform.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents a (Cuneiform) workflow. It has a name, the contents of the
 * workflow file, a list of input parameters and a list of output parameters.
 * <p>
 * @author stig
 */
@XmlRootElement
public class WorkflowDTO {

  private String name;
  private String contents;
  private List<InputParameter> inputParams;
  private List<OutputParameter> outputParams;
  
  /**
   * No-arg constructor for JAXB.
   */
  public WorkflowDTO(){}

  /**
   * Creates a new WorkflowDTO with the given name.
   * <p>
   * @param name
   */
  public WorkflowDTO(String name) {
    this(name, null);
  }

  /**
   * Creates a new WorkflowDTO with the given name and contents.
   * <p>
   * @param name
   * @param contents
   */
  public WorkflowDTO(String name, String contents) {
    this(name, contents, new ArrayList<InputParameter>(),
            new ArrayList<OutputParameter>());
  }

  /**
   * Creates a new WorkflowDTO with the given name, contents and in- and output
   * parameters.
   * <p>
   * @param name
   * @param contents
   * @param inputParams
   * @param outputParams
   */
  public WorkflowDTO(String name, String contents,
          List<InputParameter> inputParams, List<OutputParameter> outputParams) {
    this.name = name;
    this.contents = contents;
    this.inputParams = inputParams;
    this.outputParams = outputParams;
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
   * @param name 
   */
  public void setName(String name){
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
   * Add an input parameter to the list. Only happens if the list does not
   * contain this parameter yet.
   * <p>
   * @param param
   * @return True if the addition took place, false if not.
   */
  public boolean addInputParameter(InputParameter param) {
    if (!inputParams.contains(param)) {
      return inputParams.add(param);
    } else {
      return false;
    }
  }

  /**
   * Add an output parameter to the list. Only happens if the list does not
   * contain this parameter yet.
   * <p>
   * @param param
   * @return True if the addition took place, false if not.
   */
  public boolean addOutputParameter(OutputParameter param) {
    if (!outputParams.contains(param)) {
      return outputParams.add(param);
    } else {
      return false;
    }
  }

  /**
   * Remove the given input parameter.
   * <p>
   * @param param
   * @return
   */
  public boolean removeInputParameter(InputParameter param) {
    return inputParams.remove(param);
  }

  /**
   * Remove the given output parameter.
   * <p>
   * @param param
   * @return
   */
  public boolean removeOutputParameter(OutputParameter param) {
    return outputParams.remove(param);
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

}
