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
 * Controller for the serving page.
 */
'use strict';

angular.module('hopsWorksApp')
  .controller('servingCtrl', ['$scope', '$routeParams', 'growl', 'ServingService', 'UtilsService', '$location', 'ModalService', '$interval', 'StorageService', '$mdSidenav', 'DataSetService', 'KafkaService',
    function ($scope, $routeParams, growl, ServingService, UtilsService, $location, ModalService, $interval, StorageService, $mdSidenav, DataSetService, KafkaService) {

      var self = this;

      // Instance number slider setting
      self.sliderOptions = {
        value: 1,
        options: {
          floor: 1,
          ceil: 1,
          step: 1
        }
      };

      self.projectId = $routeParams.projectID;

      self.showLogs = false;
      self.kibanaUI = "";

      self.projectKafkaTopics = [];
      self.kafkaDefaultNumPartitions = 1;
      self.kafkaDefaultNumReplicas = 1;
      self.kafkaMaxNumReplicas = 1;

      self.servings = [];

      self.editServing = {};
      self.editServing.batchingEnabled = false;

      // Configuration to create a new Kafka topic for the serving
      self.createKafkaTopicDTO = {};
      self.createKafkaTopicDTO.name = "CREATE";
      self.createKafkaTopicDTO.numOfPartitions = self.kafkaDefaultNumPartitions;
      self.createKafkaTopicDTO.numOfReplicas = self.kafkaDefaultNumReplicas;

      // Configuration to not log incoming inference requests
      self.noneKafkaTopicDTO = {};
      self.noneKafkaTopicDTO.name = "NONE";
      self.noneKafkaTopicDTO.numOfPartitions = 0;
      self.noneKafkaTopicDTO.numOfReplicas = 0;

      self.activeTab = 'serving';
      self.showCreateNewServingForm = false;
      self.showAdvancedForm = false;

      self.sendingRequest = false;

      $scope.pageSize = 10;
      $scope.sortKey = 'creationTime';
      $scope.reverse = true;

      var datasetService = DataSetService(self.projectId);

      self.ignorePoll = false;
      self.createNewServingMode = false;

      this.selectFile = function () {

        ModalService.selectModelServing('lg', '*', '').then(
          function (success) {
            self.onDirSelected(success);
          },
          function (error) {
            // Users changed their minds.
          });
      };

      self.validatePath = function (modelPath, modelName) {
        // /Projects/project_name/RelativePath
        var modelPathSplits = modelPath.split("/");
        var relativePath = modelPathSplits.slice(3).join("/");
        if (typeof modelName === 'undefined') {
            modelName = modelPathSplits[modelPathSplits.length - 1];
        }

        datasetService.getContents(relativePath).then(
          function (success) {
            var versions = [];
            for (var version in success.data) {
              if (!isNaN(success.data[version].name)) {
                versions.push(success.data[version].name);
              } else {
                growl.error("Directory doesn't respect the required structure", {
                  title: 'Error',
                  ttl: 15000
                });
                return;
              }
            }

            if (versions.length === 0) {
              growl.error("The selected model doesn't have any servable version", {
                title: 'Error',
                ttl: 15000
              });
              return;
            }

            // If the validation phase has passed, update the UI
            self.editServing.modelPath = modelPath;
            self.editServing.modelName = modelName;
            self.editServing.versions = versions.sort().reverse();

            if (self.editServing.modelVersion == null) {
              self.editServing.modelVersion = self.editServing.versions[0];
            }
          },
          function (error) {
            growl.error(error.data.errorMsg, {
              title: 'Error',
              ttl: 15000
            });
          });
      };

      // Check that the directory selected follows the required structure.
      // In particular check that the children are integers (version numbers)
      self.onDirSelected = function (modelPath) {
        self.validatePath(modelPath);
      };


      self.showAdvanced = function () {
        self.showAdvancedForm = !self.showAdvancedForm;
      };


      self.hideCreateServingForm = function () {
        self.showCreateNewServingForm = false;
        self.createNewServingMode = false;
        self.editServing = {};
        self.editServing.kafkaTopicDTO = {};
        self.editServing.kafkaTopicDTO.name = "CREATE";
        self.editServing.kafkaTopicDTO.numOfPartitions = self.kafkaDefaultNumPartitions;
        self.editServing.kafkaTopicDTO.numOfReplicas = self.kafkaDefaultNumReplicas;
        self.editServing.batchingEnabled = false;
        self.versions = [];
        self.sliderOptions.value = 1;
        self.showAdvancedForm = false;
      };

      self.getAllServings = function () {
        ServingService.getAllServings(self.projectId).then(
          function (success) {
            if (!self.ignorePoll) {
              self.servings = success.data;
            }
          },
          function (error) {
            growl.error(error.data.errorMsg, {
              title: 'Error',
              ttl: 15000
            });
          });
      };

      self.filterTopics = function(topic) {
        return topic.name === self.editServing.kafkaTopicDTO.name;
      };

      self.updateKafkaDetails = function () {
        if (self.editServing.kafkaTopicDTO.name === 'CREATE' ||
          self.editServing.kafkaTopicDTO.name === 'NONE') {
          return;
        }

        KafkaService.getTopicDetails(self.projectId, self.editServing.kafkaTopicDTO.name).then(
          function (success) {
            self.editServing.kafkaTopicDTO.numOfPartitions = success.data.length;
            if (success.data.length > 0) {
              self.editServing.kafkaTopicDTO.numOfReplicas = success.data[0].replicas.length
            }
          },
          function (error) {
            growl.error(error.data.errorMsg, {
              title: 'Error',
              ttl: 15000
            });
          });
      };

      self.updateKafkaTopics = function () {
        KafkaService.getTopics(self.projectId).then(
          function (success) {
            self.projectKafkaTopics = [];
            self.projectKafkaTopics.push(self.createKafkaTopicDTO);
            self.projectKafkaTopics.push(self.noneKafkaTopicDTO);

            for (var topic in success.data) {
              if (success.data[topic].schemaName === self.kafkaSchemaName &&
                success.data[topic].schemaVersion === self.kafkaSchemaVersion) {
                self.projectKafkaTopics.push(success.data[topic]);
              }
            }

            if (typeof self.editServing.kafkaTopicDTO !== "undefined") {
              topic = self.projectKafkaTopics.filter(self.filterTopics);
              self.editServing.kafkaTopicDTO = topic[0];
              self.updateKafkaDetails();
            } else {
              self.editServing.kafkaTopicDTO = self.projectKafkaTopics[0];
            }
          },
          function (error) {
            growl.error(error.data.errorMsg, {
              title: 'Error',
              ttl: 15000
            });
          });
      };

      self.showCreateServingForm = function () {
        self.showCreateNewServingForm = true;
        self.createNewServingMode = true;
        self.updateKafkaTopics();
      };

      self.containsServingStatus = function (status) {

        for (var j = 0; j < self.servings.length; j++) {
          if (self.servings[j].status === status) {
            return true;
          }
        }

        return false;
      };

      self.getAllServings()

      self.createOrUpdate = function () {
        self.sendingRequest = true;
        self.editServing.requestedInstances = self.sliderOptions.value;

        // Check that all the fields are populated
        if (self.editServing.modelPath === "" || self.editServing.modelPath == null ||
          self.editServing.modelName === "" || self.editServing.modelName == null ||
          self.editServing.modelVersion === "" || self.editServing.modelVersion == null) {
          growl.error("Please fill out all the fields", {
            title: 'Error',
            ttl: 15000
          });
          return;
        }

        ServingService.createOrUpdate(self.projectId, self.editServing).then(
          function (success) {
            self.getAllServings();
            self.hideCreateServingForm();
            self.sendingRequest = false;
          },
          function (error) {
            if (error.data !== undefined) {
              growl.error(error.data.errorMsg, {
                title: 'Error',
                ttl: 15000
              });
            }
            self.sendingRequest = false;
          });
      };

      self.updateServing = function (serving) {
        angular.copy(serving, self.editServing);
        self.editServing.modelVersion = self.editServing.modelVersion.toString();
        self.sliderOptions.value = serving.requestedInstances;
        self.validatePath(serving.modelPath, serving.modelName);
        self.showCreateServingForm();
      };

      self.deleteServing = function (serving) {

        ServingService.deleteServing(self.projectId, serving.id).then(
          function (success) {
            self.getAllServings();
          },
          function (error) {
            growl.error(error.data.errorMsg, {
              title: 'Error',
              ttl: 15000
            });
          });
      };

      self.startOrStopServing = function (serving, action) {
        if (action === 'START') {
          self.ignorePoll = true;
          serving.status = 'Starting';
        } else {
          self.ignorePoll;
          serving.status = 'Stopping';
        }

        ServingService.startOrStop(self.projectId, serving.id, action).then(
          function (success) {
            self.ignorePoll = false;
          },
          function (error) {
            self.ignorePoll = false;
            growl.error(error.data.errorMsg, {
              title: 'Error',
              ttl: 15000
            });
          });

      };

      self.showServingLogs = function (serving) {
         var projectName = UtilsService.getProjectName();
         self.kibanaUI = "/hopsworks-api/kibana/app/kibana?projectId=" + self.projectId +
             "#/discover?_g=()&_a=(columns:!(modelname,host,log_message,'@timestamp')," +
             "index:'" + projectName.toLowerCase() + "_serving-*',interval:auto," +
             "query:(language:lucene,query:'modelname:" + serving.modelName + "'),sort:!(_score,desc))";
         self.showLogs = true;
      };

      self.showMainUI = function() {
         self.showLogs = false;
      };

      $scope.$on('$destroy', function () {
        $interval.cancel(self.poller);
      });

      self.poller = $interval(function () {
        self.getAllServings();
      }, 5000);

      self.init = function () {
        ServingService.getConfiguration().then(
          function (success) {
            self.sliderOptions.options.ceil = success.data.maxNumInstances;
            self.kafkaSchemaName = success.data.kafkaTopicSchema;
            self.kafkaSchemaVersion = success.data.kafkaTopicVersion;
          },
          function (error) {
            self.ignorePoll = false;
            growl.error(error.data.errorMsg, {
              title: 'Error',
              ttl: 15000
            });
          }
        );

        KafkaService.defaultTopicValues(self.projectId).then(
          function (success) {
            self.kafkaDefaultNumPartitions = success.data.numOfPartitions;
            self.kafkaDefaultNumReplicas = success.data.numOfReplicas;
            self.kafkaMaxNumReplicas = success.data.maxNumOfReplicas;
          },
          function (error) {
            growl.error(error.data.errorMsg, {
              title: 'Error',
              ttl: 15000
            });
          }
        )
      };
      self.init();
    }
  ]);
