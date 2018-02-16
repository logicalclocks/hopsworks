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

/**
 * @ngdoc function
 * @name hopsWorksApp.controller:WorkflowexecutionCtrl
 * @description
 * # WorkflowexecutionCtrl
 * Controller of the hopsWorksApp
 */
angular.module('hopsWorksApp')
  .controller('WorkflowExecutionCtrl',[ '$routeParams', '$location', 'growl','WorkflowExecutionService',
      function ($routeParams, $location, growl, WorkflowExecutionService) {
          var self = this;
          self.executions = [];
          var projectId = $routeParams.projectID;
          var workflowId = $routeParams.workflowID;
          var executionId = $routeParams.executionID;

          var index = function(){
              WorkflowExecutionService.index(projectId, workflowId).then(function(success){
                  console.log(success);
                  self.executions = success.data;
              },function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000})
              })
          }
          if (workflowId) {
            index();
          }

          self.selectedIndex = -1;
          self.selectedExecution = null;
          self.selectedExecutionLogs = null;
          self.hasSelectExecution = false;

          self.create = function(id){
              var wId = workflowId;
              if(id) wId = id;
              WorkflowExecutionService.create(projectId, wId).then(function(success){
                  console.log(success);
                  growl.success("Execution started", {title: 'Success', ttl: 10000});
              },function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000})
              })
          }

          self.goToShow = function (id) {
              $location.path('project/' + projectId + '/workflows/' + workflowId + '/executions/' + id);
          }

          self.goToJobs = function (id) {
              $location.path('project/' + projectId + '/workflows/' + workflowId + '/executions/' + id + '/jobs');
          }
          self.getWorkflowId = function(){
              return workflowId;
          }

          self.goToExecutions = function (id) {
              $location.path('project/' + projectId + '/workflows/' + id + '/executions');
          };
          self.selectIndex = function(index, execution){
            self.hasSelectExecution = false;
            WorkflowExecutionService.log(projectId, workflowId, execution.id).then(function(success){
              self.selectedIndex = index;
              self.selectedExecution = execution;
              self.hasSelectExecution = true;
              self.selectedExecutionLogs = success.data;
              console.log(self.selectedExecution)
              console.log(success)
            },function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000})
            })
          }
      }]);
