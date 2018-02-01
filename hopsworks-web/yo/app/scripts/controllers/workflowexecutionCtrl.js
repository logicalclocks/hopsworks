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
