'use strict';
/*
 * Controller for the job detail dialog. 
 */
angular.module('hopsWorksApp')
        .controller('HistoryDetailCtrl', ['$scope', '$modalInstance', '$interval', 'job', 'HistoryService',
          function ($scope, $modalInstance, $interval, job, HistoryService) {

            var self = this;
            self.details;
            self.config;
            this.job = job;

            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

            self.getDetails = function () {
            HistoryService.getDetailsForJob(job.yarnAppResult.id).then(
                function (success) {
                    var str = success.data.data;
                    self.details= JSON.parse(str);
                });
            };
            
            self.getConfiguration = function () {
            HistoryService.getConfigurationForJob(job.yarnAppResult.id).then(
                function (success) {
                    console.log(success.data);
                    self.config = success.data;
                });  
            };

          }]);
