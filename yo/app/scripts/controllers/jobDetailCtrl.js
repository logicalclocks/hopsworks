'use strict';
/*
 * Controller for the job detail dialog. 
 */
angular.module('hopsWorksApp')
        .controller('JobDetailCtrl', ['$modalInstance', 'growl', 'JobService', 'job', 'projectId',
          function ($modalInstance, growl, JobService, job, projectId) {

            var self = this;
            this.job = job;
            this.jobtype; //Holds the type of job.
            this.execFile; //Holds the name of the main execution file
            this.showExecutions = false;

            var getConfiguration = function () {
              JobService.getConfiguration(projectId, job.id).then(
                      function (success) {
                        self.job.runConfig = success.data;
                        self.setupInfo();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching job configuration.', ttl: 15000});
              });
            };

            var getExecutions = function () {
              JobService.getAllExecutions(projectId, job.id).then(
                      function (success) {
                        self.job.executions = success.data;
                        self.showExecutions = success.data.length > 0;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching execution history.', ttl: 15000});
              });
            };

            this.setupInfo = function () {
              if (self.job.runConfig.type == "cuneiformJobConfiguration") {
                self.jobtype = "Cuneiform";
                self.execFile = job.runConfig.wf.name;
              } else if (self.job.runConfig.type == "sparkJobConfiguration") {
                self.jobtype = "Spark";
                self.execFile = getFileName(job.runConfig.jarPath);
              } else if (self.job.runConfig.type == "adamJobConfiguration") {
                self.jobtype = "ADAM";
                self.execFile = job.runConfig.selectedCommand.command;
              }
            };

            getConfiguration();
            getExecutions();

            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);
