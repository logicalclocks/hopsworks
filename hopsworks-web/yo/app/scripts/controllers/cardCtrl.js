'use strict';

angular.module('hopsWorksApp')
        .controller('CardCtrl', ['$scope', 'ProjectService', '$routeParams',
          function ($scope, ProjectService,$routeParams) {
            var self = this;
            self.detail = [];
            var init = function (content) {
              if (content.details !== undefined) {
                console.log("No need to get detail: ", content);
                return;
              }
              content.hits.entry.forEach(function (element) {
                self.detail[element.key] = element.value;
              });

              if (content.type === "inode") {
                ProjectService.getMoreInodeInfo({id: $routeParams.projectID ,type: content.type, inodeId: content.id})
                        .$promise.then(function (success) {
                          //console.log("More info ", success);
                          self.detail["createDate"] = success.createDate;
                          self.detail["downloads"] = success.downloads;
                          self.detail["size"] = success.size;
                          self.detail["user"] = success.user;
                          self.detail["votes"] = success.votes;
                          self.detail["path"] = success.path;
                          content.user = self.detail.user;
                          content.createDate = self.detail.createDate;
                          content.size = self.detail.size;
                        }, function (error) {
                          console.log("More info error ", error);
                        });
              } else {
                ProjectService.getMoreInfo({type: content.type, inodeId: content.id})
                        .$promise.then(function (success) {
                          //console.log("More info ", success);
                          self.detail["createDate"] = success.createDate;
                          self.detail["downloads"] = success.downloads;
                          self.detail["size"] = success.size;
                          self.detail["user"] = success.user;
                          self.detail["votes"] = success.votes;
                          self.detail["path"] = success.path;
                          content.user = self.detail.user;
                          content.createDate = self.detail.createDate;
                          content.size = self.detail.size;
                        }, function (error) {
                          console.log("More info error ", error);
                        });
              }
              content.public_ds = self.detail.public_ds;
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
          }]);


