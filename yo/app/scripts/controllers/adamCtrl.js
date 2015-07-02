/**
 * Controller for the Adam jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('AdamCtrl', ['$scope','$routeParams','growl', 'JobHistoryService','$interval','AdamService', 'ModalService',
          function ($scope, $routeParams, growl, JobHistoryService, $interval,AdamService, ModalService) {
            
            //Set all the variables required to be a jobcontroller:
            //For fetching job history
            var self = this;
            this.JobHistoryService = JobHistoryService;
            this.projectId = $routeParams.projectID;
            this.jobType = 'ADAM';
            this.growl = growl;
            //For letting the user select a file
            this.ModalService = ModalService;
            this.selectFileRegex = /[^]*/; //matches anything
            this.selectFileErrorMsg = "Please select a file or folder.";
            this.onFileSelected = function (path) {
              //Set the path in the arguments.
              if(self.fileSelectionIsArgument){
                self.selectedCommand.arguments[self.fileSelectionName].value = path;
              }else{
                self.selectedCommand.options[self.fileSelectionName].value = path;
              }
              self.fileSelectionName = null;
            };
            //For job execution
            this.$interval = $interval;
            this.callExecute = function () {
              return AdamService.runJob(
                      self.selectedCommand);
            };
            this.onExecuteSuccess = function (success) {
              self.selectedCommand = null;
            };

            
            /*
             * Get all Spark job history objects for this project.
             */
            this.getSparkHistory = function () {
              getHistory(this);
            };

            this.getSparkHistory();
            
            this.selectFile = function (isArgument, name) {
              self.fileSelectionIsArgument = isArgument;
              self.fileSelectionName = name;
              selectFile(this);
            };

            this.execute = function () {
              execute(this);
            };

            this.selectJob = function (job) {
              selectJob(this, job);
            };
            
            this.selectCommand = function(command) {
              self.fileSelectionIsArgument = null;
              self.fileSelectionName = null;
              self.selectedCommand = command;
            }

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(this.poller);
            });

          }]);




