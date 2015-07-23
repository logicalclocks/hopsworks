package se.kth.bbc.jobs.model.description;

import se.kth.bbc.jobs.adam.AdamJobConfiguration;
import se.kth.bbc.jobs.cuneiform.model.CuneiformJobConfiguration;
import se.kth.bbc.jobs.model.configuration.JobConfiguration;
import se.kth.bbc.jobs.spark.SparkJobConfiguration;
import se.kth.bbc.jobs.yarn.YarnJobConfiguration;
import se.kth.bbc.project.Project;
import se.kth.hopsworks.user.model.Users;

/**
 * Allows the creation of JobDescription objects.
 * <p>
 * @author stig
 */
public class JobDescriptionFactory {

  /**
   * Get a new JobDescription object from the given JobConfiguration. The only
   * field that has been filled in is the JobConfiguration field.
   * <p>
   * @param config
   * @return
   * @throws IllegalArgumentException If the factory cannot produce a
   * JobDescription for the given configuration.
   * @throws NullPointerException If the given JobConfiguration object is null.
   */
  public static JobDescription getNewJobDescription(JobConfiguration config)
          throws IllegalArgumentException, NullPointerException {
    if (config == null) {
      throw new NullPointerException(
              "Cannot create a JobDescription from a null configuration.");
    }

    if (config instanceof AdamJobConfiguration) {
      return new AdamJobDescription((AdamJobConfiguration) config);
    } else if (config instanceof SparkJobConfiguration) {
      return new SparkJobDescription((SparkJobConfiguration) config);
    } else if (config instanceof CuneiformJobConfiguration) {
      return new CuneiformJobDescription((CuneiformJobConfiguration) config);
    } else if (config instanceof YarnJobConfiguration) {
      return new YarnJobDescription((YarnJobConfiguration) config);
    } else {
      throw new IllegalArgumentException(
              "The current Factory cannot produce a JobDescription object for "
                      + "the given configuration.");
    }
  }

  /**
   * Get a new JobDescription object from the given JobConfiguration. All given
   * fields are filled in.
   * <p>
   * @param config The configuration object describing what this job should do.
   * @param jobName The name of the job.
   * @param owner The user who created this job.
   * @param project The project in whose scope this job is created.
   * @return
   * @throws IllegalArgumentException If the factory cannot produce a
   * JobDescription for the given configuration.
   * @throws NullPointerException If the given JobConfiguration object is null.
   */
  public static JobDescription getNewJobDescription(JobConfiguration config,
          String jobName, Users owner, Project project)
          throws IllegalArgumentException, NullPointerException {
    JobDescription jd = getNewJobDescription(config);
    jd.setName(jobName);
    jd.setCreator(owner);
    jd.setProject(project);
    return jd;
  }
}
