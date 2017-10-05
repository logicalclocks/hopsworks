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


