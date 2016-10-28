'use strict';

/**
 * @ngdoc service
 * @name hopsWorksApp.WorkflowExecutionService
 * @description
 * # WorkflowExecutionService
 * Service in the hopsWorksApp.
 */
angular.module('hopsWorksApp')
  .service('WorkflowExecutionService', ['$http', function ($http) {
    return {
      index: function(projectId, workflowId){
        return $http.get('/api/project/'+ projectId + '/workflows/' + workflowId + '/executions')
      },
      create: function(projectId, workflowId){
        var regReq={
          method: 'POST',
          url: '/api/project/'+ projectId + '/workflows/' + workflowId + '/executions',
          headers: {'Content-Type': 'application/json'},
          dataType: "json"
        }
        return $http(regReq)
      },
      show: function(projectId, workflowId, id){
        return $http.get('/api/project/'+ projectId + '/workflows/' + workflowId + '/executions/' + id)
      },
      log: function(projectId, workflowId, id){
        return $http.get('/api/project/'+ projectId + '/workflows/' + workflowId + '/executions/' + id + "/logs")
      }
    }
  }]);
