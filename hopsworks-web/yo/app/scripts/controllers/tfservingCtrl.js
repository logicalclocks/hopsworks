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

/**
 * Controller for the kafka page.
 */
'use strict';

angular.module('hopsWorksApp')
        .controller('TfservingCtrl', ['$scope', '$routeParams', 'growl', 'TfServingService', '$location', 'ModalService', '$interval', 'StorageService', '$mdSidenav',
          function ($scope, $routeParams, growl, TfServingService, $location, ModalService, $interval, StorageService, $mdSidenav) {


            var self = this;
            self.projectId = $routeParams.projectID;
            self.closeAll = false;

            self.servings = [];
            self.newServing = {};

            self.activeTab = 'serving';

            self.showCreateNewServingForm = false;

            self.selectedIndex = -1;

            self.jobClickStatus = [];

            self.log = '';
            self.logId = -1;
            self.logModel = '';

            $scope.pageSize = 10;
            $scope.sortKey = 'creationTime';
            $scope.reverse = true;

            self.clickTab = function (tab) {
              if (tab === 'serving') {
                self.activeTab = 'serving'
              } else {
                self.activeTab = 'models'
              }

            };

            self.createNewServingMode = false;

            //Set some (semi-)constants
            self.selectFileRegexes = {
              "PB": /.pb\b/
            };

            self.selectFileErrorMsgs = {
              "PB": "Please select a .pb file. It should be in Models/{model_name}/{version}/model.pb"
            };

            this.selectFile = function (reason, serving, op) {
              ModalService.selectFile('lg', self.selectFileRegexes[reason.toUpperCase()], self.selectFileErrorMsgs[reason.toUpperCase()]).then(
                      function (success) {
                        if (op === 'NEW') {
                          self.onFileSelected(reason, "hdfs://" + success, 'NEW', serving);
                        }

                        if (op === 'VERSION') {
                          self.onFileSelected(reason, "hdfs://" + success, 'VERSION', serving);
                        }

                      },
                      function (error) {
                        //The user changed their mind.
                      });
            };

            self.onFileSelected = function (reason, path, op, serving) {
              var re = /(?:\.([^.]+))?$/;
              var extension = re.exec(path)[1];
              var file = path.replace(/^.*[\\\/]/, '')
              var fileName = file.substr(0, file.lastIndexOf('.'))
              switch (extension.toUpperCase()) {
                case "PB":
                  if (op === 'NEW') {
                    self.newServing.hdfsModelPath = path
                  }

                  if (op === 'VERSION') {

                    serving.hdfsModelPath = path

                    TfServingService.changeVersion(self.projectId, serving).then(
                            function (success) {
                              self.getAllServings(self.projectId);
                            },
                            function (error) {
                              growl.error(error.data.errorMsg, {
                                title: 'Error',
                                ttl: 15000
                              });
                            });

                  }


                  break;
                default:
                  growl.error("Invalid file type selected: " + extension + ", but expected .pb", {ttl: 10000});
                  break;
              }
            };


            self.showCreateServingForm = function () {
              self.showCreateNewServingForm = true
              self.createNewServingMode = true
            };

            self.getAllServings = function () {
              TfServingService.getAllServings(self.projectId).then(
                      function (success) {
                        self.servings = success.data;
                      },
                      function (error) {
                        growl.error(error.data.errorMsg, {
                          title: 'Error',
                          ttl: 15000
                        });
                      });
            };


            self.containsServingStatus = function (status) {

              for (var j = 0; j < self.servings.length; j++) {
                if (self.servings[j].status === status) {
                  return true;
                }
              }

              return false;
            };

            self.getAllServings(self.projectId)

            self.createNewServing = function () {
              TfServingService.createNewServing(self.projectId, self.newServing).then(
                      function (success) {
                        self.getAllServings(self.projectId);
                        self.showCreateNewServingForm = false;
                        self.createNewServingMode = false;

                      },
                      function (error) {
                        if (error.data !== undefined) {
                          growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                          });
                        }
                      });
            };
            
            self.deleteServing = function (serving) {

              TfServingService.deleteServing(self.projectId, serving.id).then(
                      function (success) {
                        if (serving.id === self.logId) {
                          self.logId = -1;
                          self.log = '';
                          self.logModel = '';
                        }
                        self.getAllServings(self.projectId);
                      },
                      function (error) {
                        growl.error(error.data.errorMsg, {
                          title: 'Error',
                          ttl: 15000
                        });
                      });
            };

            self.showLogs = function (serving) {
              TfServingService.getLogs(self.projectId, serving.id).then(
                      function (success) {
                        self.logId = serving.id;
                        self.log = success.data.stdout;
                        self.logModel = serving.modelName;
                      },
                      function (error) {
                        self.log = 'Internal error: could not get logs';
                        growl.error(error.data.errorMsg, {
                          title: 'Error',
                          ttl: 15000
                        });
                      });
            };

            self.startServing = function (serving) {

              serving.status = 'Starting'

              TfServingService.startServing(self.projectId, serving.id).then(
                      function (success) {
                        self.getAllServings(self.projectId);
                      },
                      function (error) {
                        self.getAllServings(self.projectId);
                        if (error.data !== undefined) {
                          growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                          });
                        }
                      });
            };

            self.stopServing = function (serving) {
              TfServingService.stopServing(self.projectId, serving.id).then(
                      function (success) {
                        self.logId = -1;
                        self.log = '';
                        self.logModel = '';
                        self.getAllServings(self.projectId)

                      },
                      function (error) {
                        self.getAllServings(self.projectId)
                        if (error.data !== undefined) {
                          growl.error(error.data.errorMsg, {
                            title: 'Error',
                            ttl: 15000
                          });
                        }
                      });
            };
            
            self.transformGraph = function (serving) {

              serving.status = 'Transforming'

              ModalService.transformGraph('lg', serving.id, self.newServing.hdfsModelPath).then(
                      function (success) {


                      },
                      function (error) {
                        //The user changed their mind.
                      });

            };
            
            

            $scope.$on('$destroy', function () {
              $interval.cancel(self.poller);
            });

            var startPolling = function () {
              self.poller = $interval(function () {
                self.getAllServings();
              }, 10000);
            };
            //startPolling();
          }
        ]);