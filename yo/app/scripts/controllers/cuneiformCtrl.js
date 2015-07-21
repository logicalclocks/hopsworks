/**
 * Created by stig on 2015-05-25.
 * Controller for the Cuneiform jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('CuneiformCtrl', ['$scope', '$routeParams', 'growl', 'ModalService', 'JobHistoryService', 'CuneiformService', '$interval',
          function ($scope, $routeParams, growl, ModalService, JobHistoryService, CuneiformService, $interval) {
            //Set all the variables required to be a jobcontroller:
            //For fetching job history
            var self = this;
            this.JobHistoryService = JobHistoryService;
            this.projectId = $routeParams.projectID;
            this.jobType = 'CUNEIFORM';
            this.growl = growl;
            //For letting the user select a file
            this.ModalService = ModalService;
            this.selectFileRegex = /.cf\b/;
            this.selectFileErrorMsg = "Please select a Cuneiform workflow. The file should have the extension '.cf'.";
            this.onFileSelected = function (path) {
              CuneiformService.inspectStoredWorkflow(this.projectId, path).then(
                      function (success) {
                        self.runConfig = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            };
            //For job execution
            this.$interval = $interval;
            this.callExecute = function () {
              return CuneiformService.runWorkflow(
                      self.projectId, self.runConfig);
            };
            this.onExecuteSuccess = function (success) {
              self.runConfig = null;
            };

            this.getHistory = function () {
              getHistory(this);
            };

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

            //Load the job history
            this.getHistory();

          }]);


