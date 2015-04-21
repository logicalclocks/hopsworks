package se.kth.bbc.jobs.cuneiform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.bean.ViewScoped;

/**
 *
 * @author stig
 */
@ManagedBean(name = "cuneiformParamsMB")
@SessionScoped
public class CuneiformParamsMB implements Serializable {

  private List<CuneiformParameter> input;
  private List<CuneiformParameter> output;

  /**
   * Creates a new instance of CuneiformParamsMB
   */
  public CuneiformParamsMB() {
    this.input = new ArrayList<>();
    this.output = new ArrayList<>();
  }

  public List<CuneiformParameter> getInput() {
    return input;
  }

  public List<CuneiformParameter> getOutput() {
    return output;
  }

  public void addInputParam(CuneiformParameter in) {
    if (!input.contains(in)) {
      this.input.add(in);
    }
  }

  public void addOutputParam(CuneiformParameter out) {
    if (!output.contains(out)) {
      this.output.add(out);
    }
  }

  public void deleteInputParam(CuneiformParameter in) {
    this.input.remove(in);
  }

  public void deleteOutputParam(CuneiformParameter out) {
    this.output.remove(out);
  }

  public void addNewInputParam() {
    CuneiformParameter in = new CuneiformParameter();
    if (!input.contains(in)) {
      this.input.add(in);
    }
  }

  public void addNewOutputParam() {
    CuneiformParameter out = new CuneiformParameter();
    if (!output.contains(out)) {
      this.output.add(out);
    }
  }

}
