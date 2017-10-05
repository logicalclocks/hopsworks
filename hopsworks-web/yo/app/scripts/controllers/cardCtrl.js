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


