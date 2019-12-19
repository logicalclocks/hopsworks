/*
 * This file is part of Hopsworks
 * Copyright (C) 2019, Logical Clocks AB. All rights reserved
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

angular.module('hopsWorksApp')
  .controller('ProvenanceCtrl', ['ProjectService', '$routeParams', '$location', 'growl',
    function (ProjectService,  $routeParams, $location, growl) {

      var self = this;
      self.provStatus = [
        {id: 'DISABLED', label: 'No provenance'},
        {id: 'META_ENABLED', label: 'Meta Enabled - Searchable'},
        {id: 'MIN_PROV_ENABLED', label: 'Minimum Provenance Enabled - Only states are saved' +
            ' (dataset/experiment/model/training_dataset/features'},
        {id: 'FULL_PROV_ENABLED', label: 'FULL Provenance Enabled - States and operations are saved' +
            ' (dataset/experiment/model/training_dataset/features'}
      ];
      self.projectId = $routeParams.projectID;
      self.pGetStatusWorking = false;
      self.dGetStatusWorking = false;
      self.setStatusWorking = false;
      self.projectProvenanceStatus = "DISABLED";
      self.datasetsProvenanceStatus = [];
      self.provFSDatasetsSize = 0;
      self.provExperimentsSize = 0;
      self.provModelsSize = 0;
      self.provTrainingDatasetsSize = 0;

      self.unmarshalProvStatus = function(response) {
        var provenanceStatus;
        switch(response.prov_status) {
          case "NONE":
            if(response.meta_status === "DISABLED") {
              provenanceStatus = "DISABLED";
            } else if(response.meta_status === "META_ENABLED") {
              provenanceStatus = "META_ENABLED";
            } break;
          case "STATE": provenanceStatus = "MIN_PROV_ENABLED"; break;
          case "ALL": provenanceStatus = "FULL_PROV_ENABLED"; break;
        }
        return provenanceStatus;
      };

      self.marshalProvStatus = function(status) {
        var provenanceStatus;
        switch(status) {
          case "DISABLED": provenanceStatus = {meta_status:"DISABLED", prov_status:"NONE"}; break;
          case "META_ENABLED": provenanceStatus = {meta_status:"META_ENABLED", prov_status:"NONE"}; break;
          case "MIN_PROV_ENABLED": provenanceStatus = {meta_status:"MIN_PROV_ENABLED", prov_status:"STATE"}; break;
          case "FULL_PROV_ENABLED": provenanceStatus = {meta_status:"FULL_PROV_ENABLED", prov_status:"ALL"}; break;
        };
        return provenanceStatus;
      };

      self.getProjectProvenanceStatus = function () {
        self.pGetStatusWorking = true;
        ProjectService.getProjectProvenanceStatus({id: self.projectId})
          .$promise.then(
            function (response) {
              self.projectProvenanceStatus = self.unmarshalProvStatus(response);
              self.pGetStatusWorking = false;
            },
            function (error) {
              if (typeof error.data.usrMsg !== 'undefined') {
                growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
              } else {
                growl.error("", {title: error.data.errorMsg, ttl: 5000});
              }
              self.pGetStatusWorking = false;
            });
      };

      self.getProjectProvenanceStatus();

      self.getDatasetsProvenanceStatus = function() {
        self.dGetStatusWorking = true;
        ProjectService.getDatasetsProvenanceStatus({id: self.projectId})
          .$promise.then(
          function (response) {
            for (var i = 0; i < response.length; i++) {
              self.datasetsProvenanceStatus[i] = {name: response[i].name, inodeId: response[i].inodeId, status: self.unmarshalProvStatus(response[i].status)};
            }
            self.dGetStatusWorking = false;
          },
          function (error) {
            if (typeof error.data.usrMsg !== 'undefined') {
              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
            } else {
              growl.error("", {title: error.data.errorMsg, ttl: 5000});
            }
            self.dGetStatusWorking = false;
          });
      };

      self.getDatasetsProvenanceStatus();

      self.isWorking = function() {
        return self.pGetStatusWorking || self.dGetStatusWorking || self.setStatusWorking;
      };

      self.getSize = function() {
        ProjectService.provStates({id: self.projectId, provStateType: "DATASET"})
          .$promise.then(
          function (response) {
            self.provFSDatasetsSize = response.result.value;
          },
          function (error) {
            if (typeof error.data.usrMsg !== 'undefined') {
              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
            } else {
              growl.error("", {title: error.data.errorMsg, ttl: 5000});
            }
          });
        ProjectService.provStates({id: self.projectId, provStateType: "EXPERIMENT"})
          .$promise.then(
          function (response) {
            self.provExperimentsSize = response.result.value;
          },
          function (error) {
            if (typeof error.data.usrMsg !== 'undefined') {
              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
            } else {
              growl.error("", {title: error.data.errorMsg, ttl: 5000});
            }
          });
        ProjectService.provStates({id: self.projectId, provStateType: "MODEL"})
          .$promise.then(
          function (response) {
            self.provModelsSize = response.result.value;
          },
          function (error) {
            if (typeof error.data.usrMsg !== 'undefined') {
              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
            } else {
              growl.error("", {title: error.data.errorMsg, ttl: 5000});
            }
          });
        ProjectService.provStates({id: self.projectId, provStateType: "TRAINING_DATASET"})
          .$promise.then(
          function (response) {
            self.provTrainingDatasetsSize = response.result.value;
          },
          function (error) {
            if (typeof error.data.usrMsg !== 'undefined') {
              growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
            } else {
              growl.error("", {title: error.data.errorMsg, ttl: 5000});
            }
          });
      }
      self.getSize();
    }]);