/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
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
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ClusterUtilisationCtrl', ['$scope', '$routeParams','$interval', 'ClusterUtilService',
          function ($scope, $routeParams, $interval, ClusterUtilService) {
            var self = this;
            self.pId = $routeParams.projectID;
            self.utilisation = -1;
            self.utilisationBar = 0;
            self.availableVCores = 0.0;
            self.allocatedVCores = 0.0;
            self.availableMB = 0.0;
            self.allocatedMB = 0.0;
            self.availableGPUs = 0;
            self.allocatedGPUs = 0;
            self.reservedGPUs = 0;
            self.gpusPercent = 0;
            self.progressBarClass = 'progress-bar-success';
            self.gpuBarClass = 'progress-bar-success';
            
            self.HdfsBan = false;
            self.HdfsWarn = false;
            self.YarnBan = false;
            self.YarnWarn = false;
            self.KafkaBan = false;
            self.KafkaWarn = false;
            
            var getClusterUtilisation = function () {
                ClusterUtilService.getYarnMetrics().then(
                      function (success) {
                        self.allocatedGPUs = success.data.clusterMetrics.allocatedGPUs;
                        self.reservedGPUs = success.data.clusterMetrics.reservedGPUs;
                        self.availableGPUs = success.data.clusterMetrics.availableGPUs;
                        var totalGpus = self.availableGPUs + self.allocatedGPUs;
                        if (self.availableGPUs > 0 && self.reservedGPUs == 0) {
                          self.gpuBarClass = 'progress-bar-success';
                        } else if (self.availableGPUs > 0 ) {
                          self.gpuBarClass = 'progress-bar-warning';
                        } else {
                          self.gpuBarClass = 'progress-bar-danger';
                        }
                        self.gpusPercent = (self.allocatedGPUs/totalGpus)*100;
                        
                        self.allocatedVCores = success.data.clusterMetrics.allocatedVirtualCores;
                        self.availableVCores = success.data.clusterMetrics.availableVirtualCores;
                        self.availableMB = success.data.clusterMetrics.availableMB;
                        self.allocatedMB = success.data.clusterMetrics.allocatedMB;
                        var totalVCores = self.allocatedVCores + self.availableVCores;
                        var totalMB = self.allocatedMB + self.availableMB;
                        var vCoreUtilisation = (self.allocatedVCores / totalVCores) * 100;
                        var MBUtilisation = (self.allocatedMB / totalMB) * 100;
                        self.utilisation = Math.round(Math.max(vCoreUtilisation, MBUtilisation))
                        self.utilisationBar = Math.max(self.utilisation, 10);
                        
                        if (self.utilisation <= 50) {
                          self.progressBarClass = 'progress-bar-success';
                        } else if (self.utilisation <= 80) {
                          self.progressBarClass = 'progress-bar-warning';
                        } else {
                          self.progressBarClass = 'progress-bar-danger';
                        }
                      }, function (error) {
                        console.log("Problem getting cluster utilization");
                        self.utilisation = -1;
                });
            };
                
                
            var getHdfsStatus = function () {
              ClusterUtilService.getHdfsStatus().then(
                      function (success) {
                        var nbDataNodes = 0;
                        var nbRunningDataNodes = 0;
                        var nbNameNodes = 0;
                        var nbRunningNameNodes = 0;
                        success.data.forEach(function (status) {
                          if (status.service === "datanode") {
                            nbDataNodes++;
                            if (status.status === "Started") {
                              nbRunningDataNodes++;
                            }
                          }
                          if (status.service === "namenode") {
                            nbNameNodes++;
                            if (status.status === "Started") {
                              nbRunningNameNodes++;
                            }
                          }
                        });
                        if (nbRunningDataNodes > nbDataNodes * 2 / 3 && nbRunningNameNodes > nbNameNodes / 2) {
                          self.HdfsBan = false;
                          self.HdfsWarn = false;
                        } else if (nbRunningDataNodes > nbDataNodes * 1 / 3 && nbRunningNameNodes >= 1) {
                          self.HdfsBan = false;
                          self.HdfsWarn = true;
                        } else {
                          self.HdfsBan = true;
                          self.HdfsWarn = false;
                        }

                      }, function (error) {
                console.log("problem getting HDFS status");
              });
            };

            var getYarnStatus = function () {
              ClusterUtilService.getYarnStatus().then(
                      function (success) {
                        var nbNodeManagers = 0;
                        var nbRunningNodeManagers = 0;
                        var nbRMs = 0;
                        var nbRunningRMs = 0;
                        success.data.forEach(function (status) {
                          if (status.service === "nodemanager") {
                            nbNodeManagers++;
                            if (status.status === "Started") {
                              nbRunningNodeManagers++;
                            }
                          }
                          if (status.service === "resourcemanager") {
                            nbRMs++;
                            if (status.status === "Started") {
                              nbRunningRMs++;
                            }
                          }
                        });
                        if (nbRunningNodeManagers > nbNodeManagers * 2 / 3 && nbRunningRMs > nbRMs / 2) {
                          self.YarnBan = false;
                          self.YarnWarn = false;
                        } else if (nbRunningNodeManagers > nbNodeManagers * 1 / 3 && nbRunningRMs >= 1) {
                          self.YarnBan = false;
                          self.YarnWarn = true;
                        } else {
                          self.YarnBan = true;
                          self.YarnWarn = false;
                        }

                      }, function (error) {
                console.log("problem getting HDFS status");
              });
            };

            var getKafkaStatus = function () {
              ClusterUtilService.getKafkaStatus().then(
                      function (success) {
                        var nbInstances = 0;
                        var nbRunningInstances = 0;
                        success.data.forEach(function (status) {
                          nbInstances++;
                          if (status.status === "Started") {
                            nbRunningInstances++;
                          }
                        });
                        if (nbRunningInstances > nbInstances * 2 / 3) {
                          self.KafkaBan = false;
                          self.KafkaWarn = false;
                        } else if (nbRunningInstances > nbInstances * 1 / 3) {
                          self.KafkaBan = false;
                          self.KafkaWarn = true;
                        } else {
                          self.KafkaBan = true;
                          self.KafkaWarn = false;
                        }

                      }, function (error) {
                console.log("problem getting HDFS status");
              });
            };
            
            var getClusterUtilisationInterval = $interval(function () {
              getClusterUtilisation();
            }, 10000);
            getClusterUtilisation();
            getHdfsStatus();
            getYarnStatus();
            getKafkaStatus();
            $scope.$on("$destroy", function () {
              $interval.cancel(getClusterUtilisationInterval);
            });
          }]);


