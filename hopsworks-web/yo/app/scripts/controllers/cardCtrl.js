/*
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
 *
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('CardCtrl', ['$scope', 'ProjectService', 'DelaService', '$routeParams', '$rootScope', '$location',
          function ($scope, ProjectService, DelaService, $routeParams, $rootScope, $location) {
            var self = this;
            self.detail = [];
            var init = function (content) {
              if (content.details !== undefined) {
                console.log("No need to get detail: ", content);
                return;
              }
              if (content.map !== undefined) {
                content.map.entry.forEach(function (element) {
                  if (self.detail[element.key] === undefined && element.value !== null) {
                    self.detail[element.key] = element.value;
                  }
                });
              }
              self.setLocalDetails = function (receivedDetails) {
                self.detail["createDate"] = receivedDetails.createDate;
                self.detail["downloads"] = receivedDetails.downloads;
                self.detail["size"] = receivedDetails.size;
                self.detail["user"] = receivedDetails.user;
                self.detail["votes"] = receivedDetails.votes;
                self.detail["path"] = receivedDetails.path;
                content.user = self.detail.user;
                content.createDate = self.detail.createDate;
                content.size = self.detail.size;
              };
              self.setHopsSiteDetails = function (receivedDetails) {
                self.detail["createDate"] = receivedDetails.data.dataset.publishedOn;
                self.detail["downloads"] = "toremove";
                self.detail["size"] = receivedDetails.data.dataset.size;
                self.detail["user"] = receivedDetails.data.dataset.owner.userDescription;
                self.detail["organization"] = receivedDetails.data.dataset.owner.clusterDescription;
                self.detail["votes"] = "tofix";
                self.detail["path"] = "toremove";
                content.user = self.detail.user;
                content.organization = self.detail.organization;
                content.createDate = self.detail.createDate;
                content.size = self.detail.size;
                content.bootstrap = receivedDetails.data.bootstrap;
                
                content["datasetHealth"] = {};
                content.datasetHealth["seeders"] = receivedDetails.data.dataset.datasetHealth.seeders;
                content.datasetHealth["leechers"] = receivedDetails.data.dataset.datasetHealth.leechers;
              };
              if (content.localDataset) {
                if (content.type === "inode") {
                  var request = {id: $routeParams.projectID, type: content.type, inodeId: content.id};
                  ProjectService.getMoreInodeInfo(request).$promise.then(function (success) {
                    //console.log("More info ", success);
                    self.setLocalDetails(success);
                  }, function (error) {
                    console.log("More info error ", error);
                  });
                } else {
                  var request = {type: content.type, inodeId: content.id};
                  ProjectService.getMoreInfo(request).$promise.then(function (success) {
                    //console.log("More info ", success);
                    self.setLocalDetails(success);
                  }, function (error) {
                    console.log("More info error ", error);
                  });
                }
              } else {
                DelaService.getDetails(content.publicId).then(function (success) {
                  //console.log("More info ", success);
                  self.setHopsSiteDetails(success);
                }, function (error) {
                  console.log("Dataset details error ", error);
                });
              }
              content.details = self.detail;
              console.log("Controller init: ", content);
            };

            $scope.$watch("content", function (newValue, oldValue) {
              init(newValue);
            });

            self.sizeOnDisk = function (fileSizeInBytes) {
              if (fileSizeInBytes === undefined) {
                return '--';
              }
              return convertSize(fileSizeInBytes);
            };
            
            self.gotoPublicDataset = function (content) {
              if ($rootScope.isDelaEnabled) {
                if (content.publicId === undefined) {
                  return;
                }
                $rootScope['publicDSId'] = content.publicId;
              } else {
                if (content.id === undefined) {
                  return;
                }
                $rootScope['publicDSId'] = content.id;
              }
              $location.path('/publicDataset');
            };
            
          }]);


