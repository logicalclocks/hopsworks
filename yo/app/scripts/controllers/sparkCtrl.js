/**
 * Controller for the Spark fatjar jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('SparkCtrl', ['$scope','$routeParams','growl', 'JobHistoryService','$interval','SparkService', 'ModalService',
          function ($scope, $routeParams, growl, JobHistoryService, $interval,SparkService, ModalService) {
            
            //Set all the variables required to be a jobcontroller:
            //For fetching job history
            var self = this;
            this.JobHistoryService = JobHistoryService;
            this.projectId = $routeParams.projectID;
            this.jobType = 'SPARK';
            this.growl = growl;
            //For letting the user select a file
            this.ModalService = ModalService;
            this.selectFileRegex = /.jar\b/;
            this.selectFileErrorMsg = "Please select a jar file.";
            this.onFileSelected = function (path) {
              this.selectedJar = getFileName(path);
              SparkService.inspectJar(this.projectId, path).then(
                      function (success) {
                        self.sparkConfig = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };
            //For job execution
            this.$interval = $interval;
            this.callExecute = function () {
              return SparkService.runJob(
                      self.projectId,
                      self.sparkConfig);
            };
            this.onExecuteSuccess = function (success) {
              self.sparkConfig = null;
              self.selectedJar = null;
            };

            
            /*
             * Get all Spark job history objects for this project.
             */
            this.getSparkHistory = function () {
              getHistory(this);
            };

            this.getSparkHistory();
            
            this.selectFile = function () {
              selectFile(this);
            };

            this.execute = function () {
              execute(this);
            };

            this.selectJob = function (job) {
              selectJob(this, job);
            };

            /**
             * Close the poller if the controller is destroyed.
             */
            $scope.$on('$destroy', function () {
              $interval.cancel(this.poller);
            });

          }]);


