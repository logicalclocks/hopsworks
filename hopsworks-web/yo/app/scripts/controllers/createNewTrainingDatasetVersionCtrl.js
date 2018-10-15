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
 * Controller for the the process of creating a new training dataset version using the modal
 */
angular.module('hopsWorksApp')
    .controller('createNewTrainingDatasetVersionCtrl', ['$uibModalInstance', 'FeaturestoreService',
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
            self.jobNoneOption = {name: "None", id : null}
            self.jobs.push(self.jobNoneOption)
            self.jobs = self.jobs.concat(jobs);
            self.trainingDatasets = trainingDatasets
            self.trainingDataset=trainingDataset
            self.trainingDatasetId = trainingDataset.id
            self.trainingDatasetNameRegexp = /^[a-zA-Z0-9-_]+$/;

            self.trainingDatasetNameWrongValue = 1
            self.trainingDatasetNameNotUnique = 1
            self.trainingDatasetDescriptionWrongValue = 1;
            self.trainingDatasetDataFormatWrongValue = 1
            self.wrong_values = 1;
            self.dependenciesNotUnique = 1
            self.working = false;
            self.dataFormats = [
                "csv", "tfrecords", "parquet", "tsv", "hdf5", "npy"
            ]
            self.trainingDatasetName = trainingDataset.name
            self.trainingDatasetDescription = trainingDataset.description
            self.dependencies = []
            for (i = 0; i < self.trainingDataset.dependencies.length; i++) {
                self.dependencies.push(self.trainingDataset.dependencies[i].path)
            }
            self.trainingDatasetFormat = self.dataFormats[self.dataFormats.indexOf(trainingDataset.dataFormat)]
            self.job;
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
                self.dependenciesNotUnique = 1
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
                for (i = 0; i < self.dependencies.length; i++) {
                    if(self.dependencies[i].substring(0,7) === "hdfs://"){
                        self.dependencies[i] = self.dependencies[i].substring(7)
                    }
                }
                var i;
                var hasDuplicates2 = (new Set(self.dependencies)).size !== self.dependencies.length;
                if(hasDuplicates2){
                    self.dependenciesNotUnique = -1
                    self.wrong_values = -1;
                }
                if (self.wrong_values === -1) {
                    self.working = false;
                    return;
                }
                var trainingDatasetJson = {
                    "name": self.trainingDatasetName,
                    "dependencies": self.dependencies,
                    "jobId": self.job.id,
                    "version": self.trainingDataset.version + 1,
                    "description": self.trainingDatasetDescription,
                    "dataFormat": self.trainingDatasetFormat
                }
                FeaturestoreService.createTrainingDataset(self.projectId, trainingDatasetJson, self.featurestore).then(
                    function (success) {
                        self.working = false;
                        $uibModalInstance.close(success)
                        growl.success("New training dataset version created", {title: 'Success', ttl: 1000});
                    }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Failed to create a new version of training dataset', ttl: 15000});
                        self.working = false;
                    });
                growl.info("Creating new training dataset version... wait", {title: 'Creating', ttl: 1000})
            };

            /**
             * Function called when the user clicks the "Data Dependency" button, opens up a modal where the user
             * can select a dataset from a file-viewer.
             */
            self.selectDataDependency = function (index) {
                ModalService.selectFile('lg', '*', '', true).then(
                    function (success) {
                        self.dependencies[index] = success
                    },
                    function (error) {
                        // Users changed their minds.
                    });
            };

            /**
             * Function called when the user press "add dependency" button in the create-feature-group form
             * Adds a new dependency to the form
             */
            self.addNewDependency = function() {
                self.dependencies.push("");
            };

            /**
             * Function called when the user press "delete dependency" button in the create-feature-group form
             * Deletes a new dependency from the form
             */
            self.removeNewDependency = function(index) {
                self.dependencies.splice(index, 1);
            };

            /**
             * Closes the modal
             */
            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
        }]);