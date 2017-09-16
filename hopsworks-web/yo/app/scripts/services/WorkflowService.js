'use strict';

/**
 * @ngdoc service
 * @name hopsWorksApp.WorkflowService
 * @description
 * # WorkflowService
 * Service in the hopsWorksApp.
 */
angular.module('hopsWorksApp')
  .factory('WorkflowService', ['$http', function ($http) {
    return {
      index: function(projectId){
        return $http.get('/api/project/'+ projectId + '/workflows')
      },
      create: function(projectId, data){
        var regReq={
          method: 'POST',
          url: '/api/project/'+ projectId + '/workflows',
          headers: {'Content-Type': 'application/json'},
          data: data,
          dataType: "json"
        }
        return $http(regReq)
      },
      show: function(projectId, id){
        return $http.get('/api/project/'+ projectId + '/workflows/' + id)
      }
    }
  }]);
