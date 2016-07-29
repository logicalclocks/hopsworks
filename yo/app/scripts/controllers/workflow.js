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
