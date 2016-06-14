'use strict';
/*
 * Controller for the job detail dialog. 
 */
angular.module('hopsWorksApp')
        .controller('HeuristicResults', ['$modalInstance', 'job', 'HistoryService',
          function ($modalInstance, job, HistoryService) {

            var self = this;
            this.job = job;
            self.detailedResult;

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
            
            self.getDetails = function () {
            HistoryService.getHeuristicsForJob(job.id).then(
                function (success) {
                    self.detailedResult = success.data.data.value;
                    
                    console.log(" Detailed Result - return from REST call");
                    console.log(self.detailedResult);
                });
            };

          }]);
