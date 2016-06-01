'use strict';
/*
 * Controller for the job detail dialog. 
 */
angular.module('hopsWorksApp')
        .controller('HistoryDetailCtrl', ['$scope', '$modalInstance', '$interval', 'job', 'HistoryService',
          function ($scope, $modalInstance, $interval, job, HistoryService) {

            var self = this;
            self.details;
            this.job = job;

            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $modalInstance.dismiss('cancel');
            };

            self.getDetails = function () {
            HistoryService.getDetailsForJob(job.id).then(
                function (success) {
                    var str = success.data.data;
                    self.details= JSON.parse(str);
                    console.log("Job ID: " + job.id);
                });
            };

          }]);
