/*
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

/**
 * Controller for the preview featuregroup view
 */
angular.module('hopsWorksApp')
    .controller('previewFeaturegroupCtrl', ['$uibModalInstance', '$scope', 'FeaturestoreService', 'growl',
        'projectId', 'featurestore', 'featuregroup',
        function ($uibModalInstance, $scope, FeaturestoreService, growl, projectId, featurestore, featuregroup) {

            /**
             * Initialize controller state
             */
            var self = this;
            self.projectId = projectId;
            self.featuregroup = featuregroup;
            self.featurestore = featurestore;
            self.schemaWorking = false;
            self.sampleWorking = false;
            self.schema ="Not fetched"
            self.sampleColumns = []
            self.sample = []
            self.notFetchedSample = true;

            $scope.pageSize = 10;


            /**
             * Function called when the user presses the "fetch sample" button
             */
            self.fetchSample = function () {
                if(self.sampleWorking){
                    return
                }
                self.sampleWorking = true
                FeaturestoreService.getFeaturegroupSample(self.projectId, self.featurestore, self.featuregroup).then(
                    function (success) {
                        self.sampleWorking = false;
                        self.notFetchedSample = false;
                        self.preprocessSample(success.data);
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch data sample', ttl: 5000});
                        self.sampleWorking = false;
                    });
            };

            /**
             * Function called when the user presses the "fetch schema" button
             */
            self.fetchSchema = function () {
                if(self.schemaWorking){
                    return
                }
                self.schemaWorking = true
                FeaturestoreService.getFeaturegroupSchema(self.projectId, self.featurestore, self.featuregroup).then(
                    function (success) {
                        self.schemaWorking = false;
                        self.schema = success.data.columns[0].value;
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to fetch featuregroup schema', ttl: 5000});
                        self.schemaWorking = false;
                    });
            };

            /**
             * Function for preprocessing the sample returned from the backend before visualizing it to the user
             *
             * @param rawSample the sample returned by the backend
             */
            self.preprocessSample = function (rawSample) {
                var columns = []
                var samples = []
                var sampleRow;
                var i;
                var j;
                if(rawSample.length > 0){
                    for (i = 0; i < rawSample[0].columns.length; i++) {
                        columns.push(rawSample[0].columns[i].name)
                    }
                }
                for (i = 0; i < rawSample.length; i++) {
                    sampleRow = {}
                    for (j = 0; j < rawSample[i].columns.length; j++) {
                        sampleRow[rawSample[i].columns[j].name] = rawSample[i].columns[j].value
                    }
                    samples.push(sampleRow)
                }
                self.sampleColumns = columns
                self.sample = samples
            }

            /**
             * Helper function to get the column width
             * @returns {number}
             */
            self.getColumnWidth= function () {
                return parseFloat(100)/parseFloat(previewFeaturegroupCtrl.sampleColumns.length)
            }


            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
        }]);

