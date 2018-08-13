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

'use strict';
/*
 * Controller for polling influxdb.
 */

angular.module('hopsWorksApp')
         .controller('VizopsCtrl', ['$scope', '$timeout', 'growl', 'JobService', '$interval', '$routeParams', '$route', 'VizopsService',

           function ($scope, $timeout, growl, JobService, $interval, $routeParams, $route, VizopsService) {

                var self = this;
                self.jobName = $routeParams.name;
                self.appId = ""; // startTime, endTime, now will be filled by init
                self.startTime = -1;
                self.endTime = -1; // application completion time
                self.now; // is the application running now?

                self.durationInterval;
                self.appinfoInterval; // checks for the app status
                self.durationLabel = "00:00:00:00";
               self.chosenGroupByInterval;
               self.groupByIntervals = ['10s', '30s', '1m', '3m', '10m', '30m', '1h'];

                var init = function() {
                    self.appId = VizopsService.getAppId();

                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                        function(success) {
                            var info = success.data;

                            self.startTime = info.startTime;
                            self.endTime = info.endTime;
                            self.now = info.now;

                            self.durationInterval = $interval(function () {
                                if (self.now) {
                                    self.durationLabel = self.formatTime(Date.now() - self.startTime);
                                } else {
                                    self.durationLabel = self.formatTime(self.endTime - self.startTime);
                                }
                            }, 1000);

                            if (self.now) {
                                self.appinfoInterval = $interval(function() {
                                    JobService.getAppInfo(VizopsService.getProjectId(), self.appId).then(
                                        function(success) {
                                            var info = success.data;

                                            self.endTime = info.endTime;
                                            self.now = info.now;

                                            if (!self.now) {
                                                $interval.cancel(self.appinfoInterval);
                                            }
                                        }, function(error) {
                                            growl.error(error.data.errorMsg, {title: 'Error fetching app info(overview).', ttl: 15000});
                                        }
                                    );
                                }, 2000);
                            }

                        }, function(error) {
                            growl.error(error.data.errorMsg, {title: 'Error fetching app info.', ttl: 15000});
                        }
                    );
                };

                init();

                self.formatTime = function (duration) {
                    var days = Math.floor(duration / (1000 * 60 * 60 * 24));
                    var day =  duration % (1000 * 60 * 60 * 24);
                    var hours = Math.floor(day / (1000 * 60 * 60));
                    var hour = day % (1000 * 60 * 60);
                    var minutes = Math.floor(hour / (1000 * 60));
                    var minute = hour % (1000 * 60);
                    var seconds = Math.floor(minute / 1000);
                    var pad = d3.format("02d");

                    return pad(days) + ":" + pad(hours) + ":" + pad(minutes) + ":" + pad(seconds);
                };

                self.onGroupByIntervalSelection = function () {
                    VizopsService.setGroupByInterval(self.chosenGroupByInterval);
                };

                $scope.$on('$destroy', function () {
                  $interval.cancel(self.durationInterval);
                  $interval.cancel(self.appinfoInterval);
                });
           }]);