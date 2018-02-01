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
 * @name hopsWorksApp.controller:WorkflowCtrl
 * @description
 * # WorkflowCtrl
 * Controller of the hopsWorksApp
 */
angular.module('hopsWorksApp')
        .controller('WorkflowCtrl', ['$routeParams', '$location', '$timeout', 'growl', 'WorkflowService', 'ModalService',
          function ($routeParams, $location, $timeout, growl, WorkflowService, ModalService) {
            var self = this;
            self.workflows = [];
            var projectId = $routeParams.projectID;
            var workflowId = $routeParams.workflowID;
            var index = function () {
              WorkflowService.index(projectId).then(function (success) {
                console.log(success);
                self.workflows = success.data;
              }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000})
              })
            }
            index();

            self.newWorkflowModal = function () {
              ModalService.newWorkflow('md').then(
                      function (success) {
                        index();
                      }, function (error) {
                index();
              });
            };
            self.goToShow = function (id) {
              $location.path('project/' + projectId + '/workflows/' + id);
            };
            self.goToExecutions = function (id) {
              $location.path('project/' + projectId + '/workflows/' + id + '/executions');
            };

            if (workflowId) {
              $timeout(function () {
                window.workflows.init() 
              }, 3000);
            }
          }]);
