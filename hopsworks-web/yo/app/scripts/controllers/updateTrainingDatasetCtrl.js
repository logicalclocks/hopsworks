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
 * Controller for the update-training-dataset-modal
 */
angular.module('hopsWorksApp')
    .controller('updateTrainingDatasetCtrl', ['$uibModalInstance', 'FeaturestoreService',
        'growl', 'projectId', 'featurestore', 'ModalService', 'jobs', '$scope', 'trainingDatasets', 'trainingDataset',
        function ($uibModalInstance, FeaturestoreService, growl, projectId, featurestore,
                  ModalService, jobs, $scope, trainingDatasets, trainingDataset) {
            /**
             * Initialize state
             */
            var self = this;
            self.projectId = projectId;
            self.featurestore = featurestore;
            self.jobs = []
            self.jobNoneOption = {name: "None", id: null}
            self.jobs.push(self.jobNoneOption)
            self.jobs = self.jobs.concat(jobs);
            self.trainingDatasets = trainingDatasets
            self.trainingDataset = trainingDataset
            self.trainingDatasetId = trainingDataset.id
            self.trainingDatasetNameRegexp = FeaturestoreService.trainingDatasetRegExp();
            self.trainingDatasetNameWrongValue = 1
            self.trainingDatasetNameNotUnique = 1
            self.trainingDatasetDescriptionWrongValue = 1;
            self.trainingDatasetDataFormatWrongValue = 1
            self.wrong_values = 1;
            self.working = false;
            self.dataFormats = FeaturestoreService.dataFormats()
            self.trainingDatasetName = trainingDataset.name
            self.trainingDatasetDescription = trainingDataset.description
            self.trainingDatasetFormat = self.dataFormats[self.dataFormats.indexOf(trainingDataset.dataFormat)]
            self.job;
            self.pageSize = 1000; //don't show pagination controls, let the user scroll instead
            self.sortKey = 'name';
            self.reverse = false;
            if('features' in trainingDataset){
                self.features = trainingDataset.features
            } else {
                self.features = []
            }

            var i;
            if (trainingDataset.jobId !== null) {
                for (i = 0; i < self.jobs.length; i++) {
                    if (self.jobs[i].id === trainingDataset.jobId) {
                        self.job = self.jobs[i];
                    }
                }
            } else {
                self.job = self.jobNoneOption
            }

            /**
             * Function called when the "update training dataset" button is pressed.
             * Validates parameters and then sends a POST request to the backend to create the new
             * training dataset
             */
            self.updateTrainingDataset = function () {
                self.trainingDatasetNameWrongValue = 1
                self.trainingDatasetNameNotUnique = 1
                self.trainingDatasetDataFormatWrongValue = 1
                self.trainingDatasetDescriptionWrongValue = 1;
                self.wrong_values = 1;
                self.working = true;
                if (!self.trainingDatasetName || self.trainingDatasetName.search(self.trainingDatasetNameRegexp) == -1 || self.trainingDatasetName.length > 256) {
                    self.trainingDatasetNameWrongValue = -1;
                    self.wrong_values = -1;
                } else {
                    self.trainingDatasetNameWrongValue = 1;
                }
                if (!self.trainingDatasetFormat || self.dataFormats.indexOf(self.trainingDatasetFormat) < 0) {
                    self.trainingDatasetDataFormatWrongValue = -1;
                    self.wrong_values = -1;
                }
                else {
                    self.trainingDatasetDataFormatWrongValue = 1;
                }
                if(!self.trainingDatasetDescription || self.trainingDatasetDescription == undefined){
                    self.trainingDatasetDescription=""
                }
                if (self.trainingDatasetDescription.length > 2000) {
                    self.trainingDatasetDescriptionWrongValue = -1;
                    self.wrong_values = -1;
                }
                else {
                    self.trainingDatasetDescriptionWrongValue = 1;
                }
                if (self.wrong_values === -1) {
                    self.working = false;
                    return;
                }
                for (i = 0; i < self.features.length; i++) {
                    if (!self.features[i].description || self.features[i].description.length == 0) {
                        self.features[i].description = "-"
                    }
                }
                var trainingDatasetJson = {
                    "name": self.trainingDatasetName,
                    "dependencies": [],
                    "jobName": self.job.name,
                    "version": self.trainingDataset.version,
                    "description": self.trainingDatasetDescription,
                    "dataFormat": self.trainingDatasetFormat,
                    "features": self.features,
                    "featureCorrelationMatrix": null,
                    "descriptiveStatistics": null,
                    "updateMetadata": true,
                    "updateStats": false,
                    "featuresHistogram": null
                }
                ModalService.confirm('sm',
                    'Caution',
                    'If you change the name of the training dataset you have to do it for each version of the training dataset manually, it is not recommended if you have more than one versions.')
                    .then(function (success) {
                        FeaturestoreService.updateTrainingDataset(self.projectId, self.trainingDatasetId, trainingDatasetJson, self.featurestore).then(
                            function (success) {
                                self.working = false;
                                $uibModalInstance.close(success)
                                growl.success("Training dataset updated", {title: 'Success', ttl: 1000});
                            }, function (error) {
                                growl.error(error.data.errorMsg, {
                                    title: 'Failed to update training dataset',
                                    ttl: 15000
                                });
                                self.working = false;
                            });
                        growl.info("Updating training dataset... wait", {title: 'Creating', ttl: 1000})
                    }, function (error) {
                        self.working = false;
                    });
            };

            /**
             * Function called when the user press "add Feature" button in the create-feature-group form
             * Adds a new feature to the form
             */
            self.addNewFeature = function () {
                self.features.push({'name': '', 'type': '', 'description': "", primary: false});
            };

            /**
             * Function called when the user press "delete Feature" button in the create-feature-group form
             * Deletes a new feature from the form
             */
            self.removeNewFeature = function (index) {
                self.features.splice(index, 1);
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
        }]);
