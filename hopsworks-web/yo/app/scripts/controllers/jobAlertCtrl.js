/*
 * This file is part of Hopsworks
 * Copyright (C) 2021, Logical Clocks AB. All rights reserved
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
    .controller('JobAlertCtrl', ['$uibModalInstance', '$routeParams', 'AlertsService', 'growl', 'projectId', 'jobName',
        function ($uibModalInstance, $routeParams, AlertsService, growl, projectId, jobName) {
            var self = this;
            self.projectId = projectId;
            self.jobName = jobName;
            var alertsService = AlertsService(self.projectId);
            self.serviceAlerts = [];
            self.values = [];
            self.loadingServiceAlerts = true;
            self.receivers = [];
            self.loadingReceivers = true;

            self.newAlert = {
              status: undefined,
              severity: undefined,
              receiver: undefined
            };

            var getMsg = function (res) {
              return (typeof res.data.usrMsg !== 'undefined')? res.data.usrMsg : '';
            }
            var getResult = function (success) {
              return typeof success !== 'undefined' && typeof success.data !== 'undefined' &&
              typeof success.data.count !== 'undefined' && success.data.count > 0 ? success.data.items : [];
            }

            var getReceivers = function() {
                self.loadingReceivers = true;
                alertsService.receivers.getAll(true).then(function (success) {
                    self.receivers = getResult(success);
                    self.loadingReceivers = false;
                });
            }

            var getServiceAlerts = function() {
              self.loadingServiceAlerts = true;
              alertsService.jobAlerts.getAll(self.jobName).then(function (success) {
                self.serviceAlerts = getResult(success);
                self.loadingServiceAlerts = false;
              });

              alertsService.jobAlerts.getValues(self.jobName).then(function (success) {
                self.values = success.data;
              });
            }

            var initAlerts = function () {
                getServiceAlerts();
                getReceivers();
            }
            initAlerts();

            self.createServiceAlert = function () {
              alertsService.jobAlerts.create(self.jobName, self.newAlert).then(function(success) {
                growl.success(getMsg(success), {title: 'Alert Created', ttl: 1000});
                getServiceAlerts();
              }, function(error){
                growl.error(getMsg(error), {title: 'Failed to create alert', ttl: 5000});
              })
            };

            self.deleteServiceAlert = function(alert) {
              alertsService.jobAlerts.delete(self.jobName, alert.id).then(function(success) {
                growl.success(getMsg(success), {title: 'Alert deleted', ttl: 1000});
                getServiceAlerts();
              }, function(error){
                growl.error(getMsg(error), {title: 'Failed to delete alert', ttl: 5000});
              })
            };

            self.testServiceAlert = function(alert) {
              alertsService.jobAlerts.test(self.jobName, alert.id).then(function(success) {
                self.alerts = getResult(success);
                growl.success(getMsg(success), {title: 'Alert sent', ttl: 1000});
              }, function(error){
                growl.error(getMsg(error), {title: 'Failed to send alert', ttl: 5000});
              })
            };

            self.close = function () {
                $uibModalInstance.dismiss('cancel');
            };
    }]);