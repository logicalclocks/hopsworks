/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

angular.module('hopsWorksApp')
        .controller('DelaCtrl', ['DelaProjectService', 'DelaClusterProjectService', '$routeParams', '$scope', '$interval', 'growl', 'ModalService',
          function (DelaProjectService, DelaClusterProjectService, $routeParams, $scope, $interval, growl, ModalService) {
            var self = this;
            self.projectId = parseInt($routeParams.projectID, 10);
            self.delaHopsService = DelaProjectService(self.projectId);
            self.preview = {};
            self.contents = [];

            self.remove = function (dataset) {
              if (dataset.torrentStatus === 'UPLOADING') {
                ModalService.confirm('sm', 'Confirm', 'Are you sure you want to make this DataSet private? \n\
                  This will make all its files unavailable to other projects unless you share it explicitly.').then(
                  function (success) {
                    self.delaHopsService.unshareFromHops(dataset.torrentId, false).then(
                      function (success) {
                        growl.success(success.data.successMessage, {title: 'The DataSet is now Private.', ttl: 1500});
                        self.preview = {};
                      }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 1500});
                    });
                  });
              } else {
                self.delaHopsService.unshareFromHops(dataset.torrentId, false).then(
                  function (success) {
                    growl.success("Download cancelled.", {title: 'Success', ttl: 1500});
                    self.preview = {};
                  }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 1500});
                });
              }
            };

            var getContents = function () {
              self.delaHopsService.datasetsInfo().then(function (success) {
                self.contents = success.data;
                if (self.contents !== undefined) {
                  var length = self.contents.length;
                  for (var j = 0; j < length; j++) {
                    if (self.contents[j].torrentStatus === "DOWNLOADING") {
                      var prevObj = self.preview[self.contents[j].torrentId.val];
                      if (prevObj === null || prevObj === undefined) {
                        prevObj = {
                          fileName: self.contents[j].fileName,
                          torrentId: self.contents[j].torrentId.val,
                          torrentStatus: self.contents[j].torrentStatus,
                          speed: 0,
                          dynamic: 0
                        };
                        self.preview[self.contents[j].torrentId.val] = prevObj;
                      }
                      self.delaHopsService.getDetails(self.contents[j].torrentId.val).then(function (success) {
                        self.preview[success.data.torrentId.val].dynamic = Math.round(success.data.percentageCompleted);
                        self.preview[success.data.torrentId.val].speed = Math.round(success.data.downloadSpeed / 1024);
                      });
                    } else {
                      prevObj = {
                        fileName: self.contents[j].fileName,
                        torrentId: self.contents[j].torrentId.val,
                        torrentStatus: self.contents[j].torrentStatus
                      };
                      self.preview[self.contents[j].torrentId.val] = prevObj;
                    }
                  }
                }
              }, function (error) {
                console.log("Error on get contents " + self.projectId, error);
              });

            };

            getContents();


            var contentsInterval = $interval(function () {
              getContents();
            }, 1000);

            $scope.$on("$destroy", function () {
              $interval.cancel(contentsInterval);
            });
          }]);
