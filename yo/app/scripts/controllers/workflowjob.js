'use strict';

/**
 * @ngdoc function
 * @name hopsWorksApp.controller:WorkflowjobCtrl
 * @description
 * # WorkflowjobCtrl
 * Controller of the hopsWorksApp
 */
angular.module('hopsWorksApp')
  .controller('WorkflowJobCtrl',[ '$routeParams', '$location', 'growl','WorkflowJobService',
      function ($routeParams, $location, growl, WorkflowJobService) {
        var self = this;
        self.jobs = [];
        var projectId = $routeParams.projectID;
        var workflowId = $routeParams.workflowID;
        var executionId = $routeParams.executionID;
        var jobId = $routeParams.jobID;

        var index = function(){
            WorkflowJobService.index(projectId, workflowId, executionId).then(function(success){
                console.log(success);
                self.jobs = success.data;
            },function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000})
            })
        }
        if(executionId) index();

        self.goToShow = function (id) {
            $location.path('project/' + projectId + '/workflows/' + workflowId + '/executions/' + executionId + '/jobs/' + id);
        }

        if(jobId){
            window.workflows.image('#image');
        }
}]);
