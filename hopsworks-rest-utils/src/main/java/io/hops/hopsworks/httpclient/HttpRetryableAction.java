/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package io.hops.hopsworks.httpclient;

import org.apache.hadoop.util.BackOff;
import org.apache.hadoop.util.ExponentialBackOff;
import org.apache.http.client.ClientProtocolException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public abstract class HttpRetryableAction<T> implements RetryableAction {
  
  private final BackOff backOffPolicy;
  
  public HttpRetryableAction() {
    this(new ExponentialBackOff.Builder()
        .setInitialIntervalMillis(500)
        .setMaximumIntervalMillis(5000)
        .setMaximumRetries(5)
        .setMultiplier(1.7));
  }
  
  public HttpRetryableAction(ExponentialBackOff.Builder backoffBuilder) {
    backOffPolicy = backoffBuilder.build();
  }
  
  public abstract T performAction() throws ClientProtocolException, IOException;
  
  @Override
  public T tryAction() throws ClientProtocolException, IOException {
    // Just to be sure
    backOffPolicy.reset();
    while (true) {
      try {
        return performAction();
      } catch (NotRetryableClientProtocolException ex) {
        throw ex;
      } catch (IOException ex) {
        long timeout = backOffPolicy.getBackOffInMillis();
        if (timeout != -1) {
          try {
            TimeUnit.MILLISECONDS.sleep(timeout);
          } catch (InterruptedException iex) {
            // Throw the original exception
            throw new ClientProtocolException(ex.getMessage(), ex);
          }
        } else {
          throw ex;
        }
      }
    }
  }
}
