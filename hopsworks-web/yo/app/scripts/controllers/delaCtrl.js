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
                            if (typeof error.data.usrMsg !== 'undefined') {
                                growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                            } else {
                                growl.error("", {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                            }
                    });
                  });
              } else {
                self.delaHopsService.unshareFromHops(dataset.torrentId, false).then(
                  function (success) {
                    growl.success("Download cancelled.", {title: 'Success', ttl: 1500});
                    self.preview = {};
                  }, function (error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                        }
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
