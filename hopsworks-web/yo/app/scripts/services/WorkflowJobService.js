'use strict';

/**
 * @ngdoc service
 * @name hopsWorksApp.WorkflowJobService
 * @description
 * # WorkflowJobService
 * Service in the hopsWorksApp.
 */
angular.module('hopsWorksApp')
  .service('WorkflowJobService', ['$http', function ($http) {
    return {
      index: function(projectId, workflowId, executionId){
        return $http.get('/api/project/'+ projectId + '/workflows/' + workflowId + '/executions/' + executionId + '/jobs')
      }
    }
  }]);
