/*
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
 *
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


