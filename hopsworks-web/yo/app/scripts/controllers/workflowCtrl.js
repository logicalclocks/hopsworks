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
