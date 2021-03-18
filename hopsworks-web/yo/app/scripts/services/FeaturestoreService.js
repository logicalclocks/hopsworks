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
        .factory('FeaturestoreService', ['$http', function ($http) {
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
                updateTrainingDatasetMetadata: function(projectId, trainingDatasetId, trainingDatasetJson, featurestore) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/trainingdatasets/" + trainingDatasetId +
                        "?updateMetadata=true&updateStats=false",
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
                 * GET request for the metadata of a particular featuregroup for a particular featurestore
                 *
                 * @param projectId project of the active user
                 * @param featurestore featurestore to get featuregroups from
                 * @param featuregroupId id of the featuregroup to get metadata for
                 * @returns {HttpPromise}
                 */
                getFeaturegroupMetadata: function(projectId, featurestore, featuregroupId) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroupId);
                },

                /**
                 * Updates the statistics settings of a featuregroup for a particular featurestore
                 *
                 * @param projectId
                 * @param featurestore
                 * @param featuregroupId
                 * @param featuregroupJson
                 * @returns {HttpPromise}
                 */
                updateFeaturegroupStatsSettings: function(projectId, featurestore, featuregroupId, featuregroupJson) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroupId +
                        "?updateStatsSettings=true",
                        JSON.stringify(featuregroupJson), {headers: {'Content-Type': 'application/json'}});
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
                 * GET request to get additional details for the feature group.   
                 *
                 * @param projectId the id of the project
                 * @param featurestore the featurestore where the featuregroup resides
                 * @param featuregroup the featuregroup to get the schema for
                 * @param storage storage for which to retrieve the details 
                 * @returns {HttpPromise}
                 */
                getFeaturegroupDetails: function(projectId, featurestore, featuregroup, storage) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroup.id 
                        + "/details?storage=" + storage);
                },

                /**
                 * GET request to preview the contents of a featuregroup (SELECT * FROM fg LIMIT X)
                 *
                 * @param projectId the id of the project
                 * @param featurestore the featurestore where the featuregroup resides
                 * @param featuregroup the featuregroup to preview
                 * @returns {HttpPromise}
                 */
                getFeaturegroupSample: function(projectId, featurestore, featuregroup, storageType, limit, partition) {
                    var partitionParam = "";
                    if (partition != null) {
                        partitionParam = "&partition=" + encodeURIComponent(partition);
                    }

                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroup.id + "/preview?storage=" +
                        storageType + "&limit=" + limit + partitionParam);
                },

                /**
                 * GET request to retrieve the partitions of a feature group 
                 * @param projectId 
                 * @param featurestore 
                 * @param featuregroup 
                 * @param storageType 
                 * @param limit 
                 */
                getFeaturegroupPartitions: function(projectId, featurestore, featuregroup) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroup.id + "/partitions");
                },

                getFeaturegroupTags: function(projectId, featurestore, featuregroup) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroup.id + "/tags");
                },

                updateFeaturegroupTag: function(projectId, featurestore, featuregroup, name, value) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + '/featuregroups/' + featuregroup.id + '/tags/' + name,
                        value, {headers: {'Content-Type': 'application/json'}});
                },

                deleteFeaturegroupTag: function(projectId, featurestore, featuregroup, tagName) {
                    return $http.delete('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroup.id + "/tags/" + tagName);
                },

                getTrainingDatasetTags: function(projectId, featurestore, td) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/trainingdatasets/" + td.id + "/tags");
                },

                updateTrainingDatasetTag: function(projectId, featurestore, td, name, value) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/trainingdatasets/" + td.id + "/tags/" + name,
                        value, {headers: {'Content-Type': 'application/json'}});
                },

                deleteTrainingDatasetTag: function(projectId, featurestore, td, tagName) {
                    return $http.delete('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/trainingdatasets/" + td.id + "/tags/" + tagName);
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
                 * PUT request to update the metadata of a featuregroup, keeping the same Hive schema
                 *
                 * @param projectId the id of the project
                 * @param featurestore the featurestore where the featuregroup resides
                 * @param featuregroupId the id of the featuregroup to
                 * @returns {HttpPromise}
                 */
                updateFeaturegroupMetadata: function(projectId, featurestore, featuregroupId, featuregroupJson) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroupId +
                        "?updateMetadata=true&updateStats=false",
                        JSON.stringify(featuregroupJson), {headers: {'Content-Type': 'application/json'}});
                },

                /**
                 * PUT request to enable online serving for a featuregroup
                 *
                 * @param projectId the id of the project
                 * @param featurestore the featurestore where the featuregroup resides
                 * @param featuregroupId the id of the featuregroup to enable online serving for
                 * @returns {HttpPromise}
                 */
                enableOnlineServing: function(projectId, featurestore, featuregroupId, featuregroupJson) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroupId +
                        "?enableOnline=true&updateMetadata=false&updateStats=false",
                        JSON.stringify(featuregroupJson), {headers: {'Content-Type': 'application/json'}});
                },

                /**
                 * PUT request to disable online serving for a featuregroup
                 *
                 * @param projectId the id of the project
                 * @param featurestore the featurestore where the featuregroup resides
                 * @param featuregroupId the id of the featuregroup to disable online feature serving for
                 * @returns {HttpPromise}
                 */
                disableOnlineServing: function(projectId, featurestore, featuregroupId, featuregroupJson) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/featuregroups/" + featuregroupId +
                        "?disableOnline=true&enableOnline=false&updateMetadata=false&updateStats=false",
                        JSON.stringify(featuregroupJson), {headers: {'Content-Type': 'application/json'}});
                },

                /**
                 * GET request for all storage connectors for a particular featurestore
                 *
                 * @param projectId project of the active user
                 * @param featurestore featurestore to get storage connectors from
                 * @returns {HttpPromise}
                 */
                getStorageConnectors: function(projectId, featurestore) {
                    return $http.get('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/storageconnectors");
                },

                /**
                 * GET request for the settings of Hopsworks featurestores
                 *
                 * @param projectId project of the active user
                 * @returns {HttpPromise}
                 */
                getFeaturestoreSettings: function(projectId) {
                    return $http.get('/api/project/' + projectId + '/featurestores/settings');
                },

                /**
                 * Sends a POST request to the backend for creating a new storage connector
                 *
                 * @param projectId project where the featuregroup will be created
                 * @param storageConnectorJson the JSON payload
                 * @param featurestore featurestore where the connector will be created
                 *
                 * @returns {HttpPromise}
                 */
                createStorageConnector: function(projectId, storageConnectorJson, featurestore) {
                    return $http.post('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/storageconnectors/",
                        JSON.stringify(storageConnectorJson), {headers: {'Content-Type': 'application/json'}});
                },

                /**
                 * Sends a PUT request to the backend for updating a storage connector
                 *
                 * @param projectId project where the featuregroup will be created
                 * @param storageConnectorJson the JSON payload
                 * @param featurestore featurestore where the connector will be created
                 * @param storageConnectorName the name of the connector
                 *
                 * @returns {HttpPromise}
                 */
                updateStorageConnector: function(projectId, storageConnectorJson, featurestore, storageConnectorName) {
                    return $http.put('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/storageconnectors/" + storageConnectorName,
                        JSON.stringify(storageConnectorJson), {headers: {'Content-Type': 'application/json'}});
                },

                /**
                 * Sends a DELETE request to the backend for deleting a Storage connector
                 *
                 * @param projectId the project of the featurestore
                 * @param featurestore the featurestore
                 * @param storageConnectorName the name of the connector
                 * @returns {HttpPromise}
                 */
                deleteStorageConnector: function(projectId, featurestore, storageConnectorName) {
                    return $http.delete('/api/project/' + projectId + '/featurestores/' +
                        featurestore.featurestoreId + "/storageconnectors/" + storageConnectorName);
                },
                /**
                 * GET request for the tags that can be attached to featuregroups or training datasets
                 *
                 * @param query string for the request
                 * @returns {HttpPromise}
                 */
                getTags: function(query) {
                    return $http.get('/api/tags' + query);
                },

                getTdQuery: function(projectId, featureStore, td) {
                    return $http.get("/api/project/" + projectId + "/featurestores/" + featureStore.featurestoreId + "/trainingdatasets/" + td.id + "/query");
                },

                constructQuery: function(projectId, queryJson) {
                    return $http.put("/api/project/" + projectId + "/featurestores/query",
                        JSON.stringify(queryJson), {headers: {'Content-Type': 'application/json'}});
                },

                computeTrainingDataset: function(projectId, featureStore, trainingDatasetId, computeJson) {
                    return $http.post("/api/project/" + projectId + "/featurestores/" + featureStore.featurestoreId + "/trainingdatasets/" + trainingDatasetId + "/compute",
                        JSON.stringify(computeJson), {headers: {'Content-Type': 'application/json'}});
                },
            };
          }]);
