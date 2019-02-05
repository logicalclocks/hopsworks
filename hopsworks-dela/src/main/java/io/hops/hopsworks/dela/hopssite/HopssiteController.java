/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.hops.hopsworks.dela.hopssite;

import com.google.gson.Gson;
import io.hops.hopsworks.common.dao.user.Users;
import io.hops.hopsworks.common.exception.RESTCodes;
import io.hops.hopsworks.common.util.ClientWrapper;
import io.hops.hopsworks.common.util.Settings;
import io.hops.hopsworks.dela.DelaStateController;
import io.hops.hopsworks.dela.dto.common.UserDTO;
import io.hops.hopsworks.dela.dto.hopssite.ClusterServiceDTO;
import io.hops.hopsworks.dela.dto.hopssite.CommentDTO;
import io.hops.hopsworks.dela.dto.hopssite.CommentIssueDTO;
import io.hops.hopsworks.dela.dto.hopssite.DatasetDTO;
import io.hops.hopsworks.dela.dto.hopssite.HopsSiteDatasetDTO;
import io.hops.hopsworks.dela.dto.hopssite.RateDTO;
import io.hops.hopsworks.dela.dto.hopssite.RatingDTO;
import io.hops.hopsworks.dela.dto.hopssite.SearchServiceDTO;
import io.hops.hopsworks.common.exception.DelaException;
import io.hops.hopsworks.dela.old_hopssite_dto.DatasetIssueDTO;
import io.hops.hopsworks.dela.old_hopssite_dto.PopularDatasetJSON;
import io.hops.hopsworks.util.SettingsHelper;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Stateless
@TransactionAttribute(TransactionAttributeType.NEVER)
public class HopssiteController {

  private static final Logger LOG = Logger.getLogger(HopssiteController.class.getName());

  @EJB
  private Settings settings;
  @EJB
  private DelaStateController delaStateCtrl;

  private void checkSetupReady() throws DelaException {
    delaStateCtrl.checkHopsworksDelaSetup();
  }

  private void checkHopssiteReady() throws DelaException {
    checkSetupReady();
    delaStateCtrl.checkHopssiteAvailable();
  }
  //********************************************************************************************************************

  private ClientWrapper getClient(String path, Class resultClass) {
    String hopsSite = settings.getHOPSSITE();
    return ClientWrapper.httpsInstance(delaStateCtrl.getKeystore(), delaStateCtrl.getTruststore(), 
      delaStateCtrl.getKeystorePassword(), new HopsSiteHostnameVerifier(settings), resultClass)
      .setTarget(hopsSite).setPath(path);
  }

  //*************************************************HEARTBEAT**********************************************************
  public String delaVersion() throws DelaException {
    checkSetupReady();
    try {
      ClientWrapper client = getClient(HopsSite.ClusterService.delaVersion(), String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:cluster - {0}", client.getFullPath());
      String result = (String) client.doGet();
      LOG.log(Settings.DELA_DEBUG, "hops-site:cluster - done {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public String registerCluster(String delaClusterAddress, String delaTransferAddress)
    throws DelaException {
    checkHopssiteReady();
    try {
      ClusterServiceDTO.Register req = new ClusterServiceDTO.Register(delaTransferAddress, delaClusterAddress);
      ClientWrapper client = getClient(HopsSite.ClusterService.registerCluster(), String.class);
      client.setPayload(req);
      LOG.log(Settings.DELA_DEBUG, "hops-site:cluster - {0}", client.getFullPath());
      String result = (String) client.doPut();
      LOG.log(Settings.DELA_DEBUG, "hops-site:cluster - done {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE,
        Level.SEVERE, DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public void heavyPing(List<String> upldDSIds, List<String> dwnlDSIds) throws DelaException {
    checkHopssiteReady();
    String publicCId = SettingsHelper.clusterId(settings);
    try {
      ClientWrapper client = getClient(HopsSite.ClusterService.heavyPing(publicCId), String.class);
      ClusterServiceDTO.HeavyPing req = new ClusterServiceDTO.HeavyPing(upldDSIds, dwnlDSIds);
      client.setPayload(req);
      LOG.log(Settings.DELA_DEBUG, "hops-site:cluster - {0}", client.getFullPath());
      String result = (String) client.doPut();
      LOG.log(Settings.DELA_DEBUG, "hops-site:cluster - done {0}", client.getFullPath());
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public void ping(ClusterServiceDTO.Ping ping) throws DelaException {
    checkHopssiteReady();
    String publicCId = SettingsHelper.clusterId(settings);
    try {
      ClientWrapper client = getClient(HopsSite.ClusterService.ping(publicCId), String.class);
      client.setPayload(ping);
      LOG.log(Settings.DELA_DEBUG, "hops-site:cluster - {0}", client.getFullPath());
      String result = (String) client.doPut();
      LOG.log(Settings.DELA_DEBUG, "hops-site:cluster -done {0}", client.getFullPath());
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  //*****************************************************USER***********************************************************
  public int registerUser(String publicCId, String firstname, String lastname, String userEmail)
    throws DelaException {
    checkHopssiteReady();
    try {
      UserDTO.Publish user = new UserDTO.Publish(firstname, lastname, userEmail);
      ClientWrapper client = getClient(HopsSite.UserService.registerUser(publicCId), String.class);
      client.setPayload(user);
      LOG.log(Settings.DELA_DEBUG, "hops-site:user - {0}", client.getFullPath());
      Integer result = Integer.parseInt((String) client.doPost());
      LOG.log(Settings.DELA_DEBUG, "hops-site:user - done {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public UserDTO.Complete getUser(String email) throws DelaException {
    checkHopssiteReady();
    String publicCId = SettingsHelper.clusterId(settings);
    try {
      ClientWrapper client = getClient(HopsSite.UserService.getUser(publicCId, email), UserDTO.Complete.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:user - {0}", client.getFullPath());
      UserDTO.Complete result = (UserDTO.Complete) client.doGet();
      LOG.log(Settings.DELA_DEBUG, "hops-site:user - done {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public Integer getUserId(String email) throws DelaException {
    checkHopssiteReady();
    String publicCId = SettingsHelper.clusterId(settings);
    try {
      ClientWrapper client = getClient(HopsSite.UserService.getUser(publicCId, email), UserDTO.Complete.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:user - {0}", client.getFullPath());
      UserDTO.Complete result = (UserDTO.Complete) client.doGet();
      LOG.log(Settings.DELA_DEBUG, "hops-site:user - done {0}", client.getFullPath());
      return result.getUserId();
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public <C extends Object> C performAsUser(Users user, HopsSite.UserFunc<C> func) throws DelaException {
    checkHopssiteReady();
    C result;
    String publicCId = SettingsHelper.clusterId(settings);
    try {
      result = func.perform();
    } catch (DelaException tpe) {
      if (RESTCodes.DelaErrorCode.USER_NOT_REGISTERED.getMessage().equals(tpe.getMessage())) {
        registerUser(publicCId, user.getFname(), user.getLname(), user.getEmail());
        result = func.perform();
      } else {
        throw tpe;
      }
    } catch (IllegalStateException ise) {
      if (RESTCodes.DelaErrorCode.USER_NOT_REGISTERED.getMessage().equals(ise.getMessage())) {
        registerUser(publicCId, user.getFname(), user.getLname(), user.getEmail());
        result = func.perform();
      } else {
        throw ise;
      }
    }
    return result;
  }

  //****************************************************TRACKER********************************************************
  public String publish(String datasetName, String description, Collection<String> categories, 
    long size, String userEmail) throws DelaException {
    checkHopssiteReady();
    String publicCId = SettingsHelper.clusterId(settings);
    try {
      DatasetDTO.Proto msg = new DatasetDTO.Proto(datasetName, description, categories, size, userEmail);
      ClientWrapper client = getClient(HopsSite.DatasetService.publish(publicCId), String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - {0}", client.getFullPath());
      client.setPayload(msg);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - done {0}", client.getFullPath());
      String result = (String) client.doPost();
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public void download(String publicDSId) throws DelaException {
    checkHopssiteReady();
    String publicCId = SettingsHelper.clusterId(settings);

    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.download(publicCId, publicDSId), String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - {0}", client.getFullPath());
      client.doPost();
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - done {0}", client.getFullPath());
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public void complete(String publicDSId) throws DelaException {
    checkHopssiteReady();

    String publicCId = SettingsHelper.clusterId(settings);

    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.complete(publicCId, publicDSId), String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - {0}", client.getFullPath());
      client.doPost();
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - done {0}", client.getFullPath());
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public void cancel(String publicDSId) throws DelaException {
    checkHopssiteReady();

    String publicCId = SettingsHelper.clusterId(settings);

    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.remove(publicCId, publicDSId), String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - {0}", client.getFullPath());
      client.doPost();
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - done {0}", client.getFullPath());
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  //*****************************************************SEARCH*********************************************************
  public SearchServiceDTO.SearchResult search(String searchTerm) throws DelaException {
    checkHopssiteReady();
    try {
      SearchServiceDTO.Params req = new SearchServiceDTO.Params(searchTerm);
      ClientWrapper client = getClient(HopsSite.DatasetService.search(), SearchServiceDTO.SearchResult.class);
      client.setPayload(req);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - {0}", client.getFullPath());
      SearchServiceDTO.SearchResult result = (SearchServiceDTO.SearchResult) client.doPost();
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset:done -  {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public SearchServiceDTO.Item[] page(String sessionId, int startItem, int nrItems) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.searchPage(sessionId, startItem, nrItems),
        String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - {0}", client.getFullPath());
      String auxResult = (String) client.doGet();
      SearchServiceDTO.Item[] result = new Gson().fromJson(auxResult, SearchServiceDTO.Item[].class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset:done - {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public SearchServiceDTO.ItemDetails details(String publicDSId) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.details(publicDSId), String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - {0}", client.getFullPath());
      String auxResult = (String) client.doGet();
      SearchServiceDTO.ItemDetails result = new Gson().fromJson(auxResult, SearchServiceDTO.ItemDetails.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset:done - {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  //*************************************************SETTINGS CHECK*****************************************************
  public boolean updateUser(UserDTO.Publish userDTO) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.UserService.user(), String.class);
      client.setPayload(userDTO);
      String res = (String) client.doPut();
      return "OK".equals(res);
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public boolean deleteUser(Integer uId) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.UserService.user() + "/" + uId, String.class);
      String res = (String) client.doDelete();
      return "OK".equals(res);
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  //***********************************************COMMENT PUBLIC*******************************************************
  public List<CommentDTO.RetrieveComment> getDatasetAllComments(String publicDSId) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.CommentService.getDatasetAllComments(publicDSId), String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:get:all {0}", client.getFullPath());
      String aux = (String) client.doGet();
      CommentDTO.RetrieveComment[] result = new Gson().fromJson(aux, CommentDTO.RetrieveComment[].class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:get:all - done {0}", client.getFullPath());
      return Arrays.asList(result);
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public void addComment(String publicCId, String publicDSId, CommentDTO.Publish comment) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.CommentService.addComment(publicCId, publicDSId), String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:add {0}", client.getFullPath());
      client.setPayload(comment);
      client.doPost();
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:add - done {0}", client.getFullPath());
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE,
        Level.SEVERE, DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public void updateComment(String publicCId, String publicDSId, Integer commentId, CommentDTO.Publish comment)
    throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.CommentService.updateComment(publicCId, publicDSId, commentId),
        String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:udpate {0}", client.getFullPath());
      client.setPayload(comment);
      client.doPut();
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:udpate - done {0}", client.getFullPath());
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public void removeComment(String publicCId, String publicDSId, Integer commentId, String userEmail)
    throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.CommentService.removeComment(publicCId, publicDSId, commentId),
        String.class)
        .setPayload(userEmail);
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:remove {0}", client.getFullPath());
      client.doPost();
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:remove - done {0}", client.getFullPath());
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public void reportComment(String publicCId, String publicDSId, Integer commentId, CommentIssueDTO commentIssue)
    throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.CommentService.reportComment(publicCId, publicDSId, commentId),
        String.class);
      client.setPayload(commentIssue);
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:report {0}", client.getFullPath());
      client.doPost();
      LOG.log(Settings.DELA_DEBUG, "hops-site:comment:report - done {0}", client.getFullPath());
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  //**************************************************RATING PUBLIC*****************************************************
  public RatingDTO getDatasetAllRating(String publicDSId) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.RatingService.getDatasetAllRating(publicDSId), RatingDTO.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:all - {0}", client.getFullPath());
      RatingDTO result = (RatingDTO) client.doGet();
      LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:all - done {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  //**************************************************RATING CLUSTER****************************************************
  public RatingDTO getDatasetUserRating(String publicCId, String publicDSId, String email) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client
        = getClient(HopsSite.RatingService.getDatasetUserRating(publicCId, publicDSId), RatingDTO.class)
        .setPayload(email);
      LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:user - {0}", client.getFullPath());
      RatingDTO result = (RatingDTO) client.doPost();
      LOG.log(Settings.DELA_DEBUG, "hops-site:rating:get:user - done {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public boolean addRating(String publicCId, String publicDSId, RateDTO datasetRating) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.RatingService.addRating(publicCId, publicDSId), String.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:rating:add - {0}", client.getFullPath());
      client.setPayload(datasetRating);
      client.doPost();
      LOG.log(Settings.DELA_DEBUG, "hops-site:rating:add - done {0}", client.getFullPath());
      return true;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  //************************************************RATING FUTURE*******************************************************
  public List<RateDTO> getAllRatingsByPublicId(String publicId) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client
        = getClient(HopsSite.RatingService.getDatasetAllByPublicId() + "/" + publicId, RateDTO.class);
      return (List<RateDTO>) client.doGetGenericType();
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public boolean updateRating(RateDTO datasetRating) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.RatingService.rating(), String.class);
      client.setPayload(datasetRating);
      String res = (String) client.doPut();
      return "OK".equals(res);
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public boolean deleteRating(Integer ratingId) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.RatingService.rating() + "/" + ratingId, String.class);
      String res = (String) client.doDelete();
      return "OK".equals(res);
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  // dataset services
  public List<HopsSiteDatasetDTO> getAll() throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.dataset(), HopsSiteDatasetDTO.class);
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - {0}", client.getFullPath());
      List<HopsSiteDatasetDTO> result = (List<HopsSiteDatasetDTO>) client.doGetGenericType();
      LOG.log(Settings.DELA_DEBUG, "hops-site:dataset - done {0}", client.getFullPath());
      return result;
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public DatasetDTO.Complete getDataset(String publicDSId) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.datasetByPublicId() + "/" + publicDSId,
        DatasetDTO.Complete.class);
      return (DatasetDTO.Complete) client.doGet();
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public SearchServiceDTO.ItemDetails getDatasetDetails(String publicDSId) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.dataset() + "/" + publicDSId + "/details",
        SearchServiceDTO.ItemDetails.class);
      return (SearchServiceDTO.ItemDetails) client.doGet();
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public boolean addDatasetIssue(DatasetIssueDTO datasetIssue) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.datasetIssue(), String.class);
      client.setPayload(datasetIssue);
      String res = (String) client.doPost();
      return "OK".equals(res);
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public boolean addCategory(DatasetDTO dataset) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.datasetCategory(), String.class);
      client.setPayload(dataset);
      String res = (String) client.doPost();
      return "OK".equals(res);
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public List<PopularDatasetJSON> getPopularDatasets() throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.datasetPopular(), PopularDatasetJSON.class);
      return (List<PopularDatasetJSON>) client.doGetGenericType();
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public boolean addPopularDatasets(PopularDatasetJSON popularDatasetsJson) throws DelaException {
    checkHopssiteReady();
    try {
      ClientWrapper client = getClient(HopsSite.DatasetService.datasetPopular(), String.class);
      client.setPayload(popularDatasetsJson);
      String res = (String) client.doPost();
      return "OK".equals(res);
    } catch (IllegalStateException ise) {
      throw new DelaException(RESTCodes.DelaErrorCode.COMMUNICATION_FAILURE, Level.SEVERE,
        DelaException.Source.HOPS_SITE, null, ise.getMessage(), ise);
    }
  }

  public String hopsSite() throws DelaException {
    return SettingsHelper.hopsSite(settings);
  }

  public static class HopsSiteHostnameVerifier implements HostnameVerifier {

    private final Settings settings;

    public HopsSiteHostnameVerifier(Settings settings) {
      this.settings = settings;
    }

    @Override
    public boolean verify(String host, SSLSession ssls) {
      String hopssite = settings.getHOPSSITE_HOST();
      if(hopssite != null) {
        return hopssite.equals(host);
      }
      return true; //TODO Alex - should return false; but test it - we always set the hopssite though
    }
  }
}
