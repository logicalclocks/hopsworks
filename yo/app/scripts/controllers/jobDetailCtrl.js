'use strict';
/*
 * Controller for the job detail dialog. 
 */
angular.module('hopsWorksApp')
        .controller('JobDetailCtrl', ['$modalInstance', 'growl', 'JobService', 'job', 'projectId',
          function ($modalInstance, growl, JobService, job, projectId) {

            var self = this;
            this.job = job;

            var getConfiguration = function () {
              JobService.getConfiguration(projectId, job.id).then(
                      function (success) {
                        self.job.runConfig = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching job configuration.', ttl: 15000});
              });
            };

            var getExecutions = function () {
              JobService.getAllExecutions(projectId, job.id).then(
                      function (success) {
                        self.job.executions = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error fetching execution history.', ttl: 15000});
              });
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
