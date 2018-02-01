/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';

angular.module('hopsWorksApp')
        .factory('HopssiteService', ['$http', function ($http) {
            var service = {
              getServiceInfo: function (serviceName) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/services/' + serviceName
                });
              },
              getClusterId: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/clusterId'
                });
              },
              getUserId: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/userId'
                });
              },
              getAll: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/datasets?filter=ALL'
                });
              },
              getTopRated: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/datasets?filter=TOP_RATED'
                });
              },
              getNew: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/datasets?filter=NEW'
                });
              },
              getType: function (filter) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/datasets?filter=' + filter
                });
              },
              getDataset: function (publicDSId) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/datasets/' + publicDSId
                });
              },
              getLocalDatasetByPublicId: function (publicDSId) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/datasets/' + publicDSId + '/local'
                });
              },
              
              getDisplayCategories: function () {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/categories'
                });
              },
              postDatasetIssue: function (publicDSId, issue) {
                return $http({
                  method: 'post',
                  url: '/api/hopssite/datasets/' + publicDSId + '/issue',
                  headers: {
                   'Content-Type': 'application/json'
                  },
                  data: issue
                });
              },
              getComments: function (publicDSId) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/datasets/' + publicDSId + '/comments'
                });
              },
              postComment: function (publicDSId, comment) {
                return $http({
                  method: 'post',
                  url: '/api/hopssite/datasets/' + publicDSId + '/comments',
                  data: comment
                });
              },
              updateComment: function (publicDSId, commentId, comment) {
                return $http({
                  method: 'put',
                  url: '/api/hopssite/datasets/' + publicDSId + '/comments/' + commentId,
                  data: comment
                });
              },
              deleteComment: function (publicDSId, commentId) {
                return $http({
                  method: 'delete',
                  url: '/api/hopssite/datasets/' + publicDSId + '/comments/' + commentId
                });
              },
              postCommentIssue: function (publicDSId, commentId, issue) {
                return $http({
                  method: 'post',
                  url: '/api/hopssite/datasets/' + publicDSId + '/comments/' + commentId + '/report',
                  headers: {
                   'Content-Type': 'application/json'
                  },
                  data: issue
                });
              },
              getRating: function (publicDSId) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/datasets/' + publicDSId + '/rating?filter=DATASET'
                });
              },
              getUserRating: function (publicDSId, user) {
                return $http({
                  method: 'get',
                  url: '/api/hopssite/datasets/' + publicDSId + '/rating?filter=USER'
                });
              },
              postRating: function (publicDSId, ratingVal) {
                var ratingValue = {"value": ratingVal};
                return $http({
                  method: 'post',
                  url: '/api/hopssite/datasets/' + publicDSId + '/rating',
                  headers: {
                   'Content-Type': 'application/json'
                  },
                  data: ratingValue
                });
              },
              getReadmeByInode: function (inodeId) {
                return $http.get('/api/project/readme/byInodeId/' + inodeId);
              }
            };
            return service;
          }]);


