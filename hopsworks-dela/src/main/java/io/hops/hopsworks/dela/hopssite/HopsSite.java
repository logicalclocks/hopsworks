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

import io.hops.hopsworks.common.exception.DelaException;

public class HopsSite {

  public static class ClusterService {

    public static String delaVersion() {
      return "public/cluster/dela/version";
    }

    public static String registerCluster() {
      return "private/cluster/register";
    }

    public static String heavyPing(String publicCId) {
      return "private/cluster/heavyPing/" + publicCId;
    }

    public static String ping(String publicCId) {
      return "private/cluster/ping/" + publicCId;
    }
  }

  public static class UserService {
    public static String user() {
      return "private/user";
    }
    
    public static String registerUser(String publicCId) {
      return "private/user/register/" + publicCId;
    }

    public static String getUser(String publicCId, String email) {
      return "private/user/" + publicCId + "/" + email;
    }
  }

  public static class DatasetService {

    public static String dataset() {
      return "private/dataset";
    }
    
    public static String datasetByPublicId() {
      return "private/dataset/byPublicId";
    }
    
    public static String datasetIssue() {
      return "private/dataset/issue";
    }
    
    public static String datasetCategory() {
      return "private/dataset/category";
    }
    
    public static String datasetPopular() {
      return "private/dataset/popular";
    }
    
    public static String publish(String publicCId) {
      return "private/dataset/publish/" + publicCId;
    }

    public static String unpublish(String publicCId, String publicDSId) {
      return "private/dataset/unpublish/" + publicCId + "/" + publicDSId;
    }

    public static String download(String publicCId, String publicDSId) {
      return "private/dataset/download/" + publicCId + "/" + publicDSId;
    }

    public static String complete(String publicCId, String publicDSId) {
      return "private/dataset/complete/" + publicCId + "/" + publicDSId;
    }

    public static String remove(String publicCId, String publicDSId) {
      return "private/dataset/remove/" + publicCId + "/" + publicDSId;
    }

    public static String search() {
      return "public/dataset/search";
    }

    public static String searchPage(String sessionId, Integer startItem, Integer nrItems) {
      return "public/dataset/search/" + sessionId + "/page/" + startItem + "/" + nrItems;
    }

    public static String details(String publicDSId) {
      return "public/dataset/" + publicDSId + "/details";
    }
  }

  public static class RatingService {
    
    public static String rating() {
      return "private/rating";
    }
    
    public static String getDatasetAllByPublicId() {
      return "private/rating/all/byPublicId";
    }
    public static String getDatasetAllRating(String publicDSId) {
      return "private/rating/dataset/" + publicDSId + "/all";
    }
    
    public static String getDatasetUserRating(String publicCId, String publicDSId) {
      return "private/rating/cluster/" + publicCId + "/dataset/" + publicDSId + "/user";
    }
    
    public static String addRating(String publicCId, String publicDSId) {
      return "private/rating/cluster/" + publicCId + "/dataset/" + publicDSId + "/add";
    }
  }
  
  public static class CommentService {
    public static String getDatasetAllComments(String publicDSId) {
      return "private/comment/dataset/" + publicDSId + "/all";
    }
    
    public static String addComment(String publicCId, String publicDSId) {
      return "private/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/add";
    }
    
    public static String updateComment(String publicCId, String publicDSId, Integer commentId) {
      return "private/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/update/" + commentId; 
    }
    
    public static String removeComment(String publicCId, String publicDSId, Integer commentId) {
      return "private/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/delete/" + commentId; 
    }
    
    public static String reportComment(String publicCId, String publicDSId, Integer commentId) {
      return "private/comment/cluster/" + publicCId + "/dataset/" + publicDSId + "/report/" + commentId;
    }
  }

  public static interface UserFunc<C extends Object> {

    public C perform() throws DelaException;
  }
}
