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
