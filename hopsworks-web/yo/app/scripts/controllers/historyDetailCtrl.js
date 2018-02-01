/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

'use strict';
/*
 * Controller for the job detail dialog. 
 */
angular.module('hopsWorksApp')
        .controller('HistoryDetailCtrl', ['$scope', '$uibModalInstance', '$interval', 'job', 'HistoryService',
          function ($scope, $uibModalInstance, $interval, job, HistoryService) {

            var self = this;
            self.details;
            self.config;
            this.job = job;

            /**
             * Close the modal dialog.
             * @returns {undefined}
             */
            self.close = function () {
              $uibModalInstance.dismiss('cancel');
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
