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

'use strict';
/*
 * Service for the feature store page
 */
angular.module('hopsWorksApp')
        .factory('FeaturestoreService', ['$http', 'TransformRequest', function ($http, TransformRequest) {
            return {

                /**
                 * Sends a POST request to the backend for creating a feature group
                 *
                 * @param projectId project where the featuregroup will be created
                 * @param featuregroupJson the JSON payload
                 * @param featurestore featurestre where the featuregroup will be created
                 * @returns {HttpPromise}
                 */
                createFeaturegroup: function(projectId, featuregroupJson, featurestore) {
                    return $http.post('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups",
                        JSON.stringify(featuregroupJson), {headers: {'Content-Type': 'application/json'}});
                },

                /**
                 * Sends a POST request to the backend for creating a managed training set
                 *
                 * @param projectId the id of the project where the managed training set will be created
                 * @param trainingDatasetJson the JSON payload
                 * @param featurestore the featurestore linked to the training dataset
                 * @returns {HttpPromise}
                 */
                createTrainingDataset: function(projectId, trainingDatasetJson, featurestore) {
                    return $http.post('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/trainingdatasets",
                        JSON.stringify(trainingDatasetJson), {headers: {'Content-Type': 'application/json'}});
                },

                /**
                 * Sends a PUT request to the backend for updating the metadata of a training dataset
                 *
                 * @param projectId the project of the user making the request
                 * @param trainingDatasetJson the json payload with the updated metadata
                 * @param featurestore the featurestore linked to the training dataset
                 * @returns {HttpPromise}
                 */
                updateTrainingDataset: function(projectId, trainingDatasetId, trainingDatasetJson, featurestore) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/trainingdatasets/" + trainingDatasetId,
                        JSON.stringify(trainingDatasetJson), {headers: {'Content-Type': 'application/json'}});
                },

                /**
                 * GET request for all featurestores for a particular project
                 *
                 * @param projectId id of the project
                 */
                getFeaturestores: function(projectId) {
                    return $http.get('/api/project/' + projectId + '/featurestores');
                },

                /**
                 * GET request for all featuregroups for a particular featurestore
                 *
                 * @param projectId project of the active user
                 * @param featurestore featurestore to get featuregroups from
                 * @returns {HttpPromise}
                 */
                getFeaturegroups: function(projectId, featurestore) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups");
                },

                /**
                 * GET request for all training datasets for a particular featurestore
                 *
                 * @param projectId project of the active user
                 * @param featurestore featurestore to get training datasets from
                 * @returns {HttpPromise}
                 */
                getTrainingDatasets: function(projectId, featurestore) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/trainingdatasets");
                },

                /**
                 * DELETE request for a specific featuregroup for a particular featurestore
                 *
                 * @param projectId project of the active user
                 * @param featurestore featurestore of the featuregroup
                 * @param featuregroupId id of the feauturegroup to delete
                 * @returns {HttpPromise}
                 */
                deleteFeaturegroup: function(projectId, featurestore, featuregroupId) {
                    return $http.delete('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroupId);
                },

                /**
                 * DELETE request for a specific training dataset for a particular featurestore
                 *
                 * @param projectId project of the active user
                 * @param featurestore featurestore of the training dataset
                 * @param trainingDatasetId id of the trainingdataset to delete
                 * @returns {HttpPromise}
                 */
                deleteTrainingDataset: function(projectId, featurestore, trainingDatasetId) {
                    return $http.delete('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/trainingdatasets/" + trainingDatasetId);
                },

                /**
                 * GET request to get the SQL schema of a featuregroup (SHOW CREATE TABLE)
                 *
                 * @param projectId the id of the project
                 * @param featurestore the featurestore where the featuregroup resides
                 * @param featuregroup the featuregroup to get the schema for
                 * @returns {HttpPromise}
                 */
                getFeaturegroupSchema: function(projectId, featurestore, featuregroup) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroup.id + "/schema");
                },

                /**
                 * GET request to preview the contents of a featuregroup (SELECT * FROM fg LIMIT X)
                 *
                 * @param projectId the id of the project
                 * @param featurestore the featurestore where the featuregroup resides
                 * @param featuregroup the featuregroup to preview
                 * @returns {HttpPromise}
                 */
                getFeaturegroupSample: function(projectId, featurestore, featuregroup) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroup.id + "/preview");
                },

                /**
                 * POST request to delete the contents of the featuregroup
                 *
                 * @param projectId the id of the project
                 * @param featurestore the featurestore containing the featuregroup
                 * @param featuregroup the featuregroup to clear the contents of
                 * @returns {HttpPromise}
                 */
                clearFeaturegroupContents: function(projectId, featurestore, featuregroup) {
                    return $http.post('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroup.id + "/clear");
                },

                /**
                 * POST request to update the metadata of a featuregroup, keeping the same Hive schema
                 *
                 * @param projectId the id of the project
                 * @param featurestore the featurestore where the featuregroup resides
                 * @param featuregroupId the id of the featuregroup to
                 * @returns {HttpPromise}
                 */
                updateFeaturegroupMetadata: function(projectId, featurestore, featuregroupId, featuregroupJson) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroupId,
                        JSON.stringify(featuregroupJson), {headers: {'Content-Type': 'application/json'}});
                }
            };
          }]);
