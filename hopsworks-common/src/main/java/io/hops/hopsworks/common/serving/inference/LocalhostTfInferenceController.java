/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
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

package io.hops.hopsworks.common.serving.inference;

import com.google.common.base.Strings;
import io.hops.common.Pair;
import io.hops.hopsworks.common.dao.serving.TfServing;
import io.hops.hopsworks.common.exception.RESTCodes;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.util.logging.Level;

import static io.hops.hopsworks.common.serving.tf.LocalhostTfServingController.PID_STOPPED;

@Alternative
@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class LocalhostTfInferenceController implements TfInferenceController {

  private OkHttpClient httpClient = new OkHttpClient();

  public Pair<Integer, String> infer(TfServing tfServing, Integer modelVersion,
                                     String verb, String inferenceRequestJson) throws InferenceException {

    if (tfServing.getLocalPid().equals(PID_STOPPED)) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.SERVINGNOTRUNNING, Level.FINE);
    }

    if (Strings.isNullOrEmpty(verb)) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.MISSING_VERB, Level.FINE);
    }

    // TODO(Fabio) does Tf model server support TLS?
    StringBuilder requestUrlBuilder = new StringBuilder();
    requestUrlBuilder.append("http://localhost:")
        .append(tfServing.getLocalPort())
        .append("/v1/models/")
        .append(tfServing.getModelName());

    // Append the version if the user specified it.
    if (modelVersion != null) {
      requestUrlBuilder.append("/versions").append(modelVersion);
    }

    requestUrlBuilder.append(verb);

    RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), inferenceRequestJson);
    Request request = new Request.Builder()
        .url(requestUrlBuilder.toString())
        .post(body)
        .build();

    Response response = null;
    try {
      response = httpClient.newCall(request).execute();
    } catch (IOException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.REQUESTERROR, Level.INFO, "", e.getMessage(), e);
    }

    if (response == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.EMPTYRESPONSE, Level.INFO, "Received null response");
    }

    if (response.body() == null) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.EMPTYRESPONSE, Level.INFO, "Received null response");
    }

    try {
      // Return prediction
      return new Pair<>(response.code(), response.body().string());
    } catch (IOException e) {
      throw new InferenceException(RESTCodes.InferenceErrorCode.ERRORREADINGRESPONSE, Level.INFO,
          "", e.getMessage(), e);
    } finally {
      response.close();
    }
  }
}
