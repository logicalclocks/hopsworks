/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.hopsworks.batch.executor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.batch.runtime.BatchRuntime;
import org.apache.log4j.Logger;

// TODO - start this with an EJBTimer
public class AuthorizedKeysJob implements Runnable {

  public static List<Long> mExecutedBatchs = new ArrayList<>();
  private static final Logger logger = Logger.getLogger(AuthorizedKeysJob.class);
  public static final String AUTHORIZED_KEYS_JOB = "authorizedKeysJob";

  @Override
  public void run() {
    logger.debug("Start the job:  " + AUTHORIZED_KEYS_JOB);
    mExecutedBatchs.add(BatchRuntime.getJobOperator().start(AUTHORIZED_KEYS_JOB, new Properties()));
  }

}
