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
    .controller('AlertManagerCtrl', ['$routeParams', 'AlertsService', 'growl',
        function ($routeParams, AlertsService, growl) {
            const GLOBAL_RECEIVER = "global-receiver__";
            var self = this;
            self.projectId = $routeParams.projectID;
            var alertsService = AlertsService(self.projectId);
            self.alerts = [];
            self.serviceAlerts = [];
            self.routes = [];
            self.receivers = [];
            self.silences = [];
            self.values = [];
            self.loadingAlerts = true;
            self.loadingServiceAlerts = true;
            self.loadingRoutes = true;
            self.loadingReceivers = true;
            self.loadingSilences = true;
            self.receiver = undefined;

            self.statusReceiver = {};
            self.statusRoute = {};
            self.statusSilences = {};
            self.statusAlerts = {};

            self.routeWorking = false;
            self.receiverWorking =false;
            self.silenceWorking =false;

            self.receiverConfigs = [
                "EmailConfigs",
                "SlackConfigs",
                "PagerdutyConfigs",
                "PushoverConfigs",
                "OpsgenieConfigs",
                "WebhookConfigs",
                "VictoropsConfigs",
                "WechatConfigs"
            ];

            self.newAlert = {
                status: undefined,
                severity: undefined,
                service: undefined,
                receiver: undefined
            };

            self.newRoute = {
                groupBy: undefined,
                groupWait: undefined,
                groupInterval: undefined,
                repeatInterval: undefined,
                receiver: undefined,
                continue: false,
                match: [],
                matchRe: []
            };
            self.newReceiver = {
                name: undefined,
                emailConfigs: undefined,
                slackConfigs: undefined,
                pagerdutyConfigs: undefined,
                pushoverConfigs: undefined,
                opsgenieConfigs: undefined,
                webhookConfigs: undefined,
                victoropsConfigs: undefined,
                wechatConfigs: undefined
            };
            self.receiverConfig = "EmailConfigs";

            self.newSilence = {
                comment: undefined,
                startsAt: undefined,
                endsAt: undefined,
                matchers: [{isRegex: false, name: undefined, value: undefined}]
            };

            self.matcher = {isRegex: false, name: undefined, value: undefined};

            var getMsg = function (res) {
                return (typeof res.data.usrMsg !== 'undefined')? res.data.usrMsg : '';
            }
            var getResult = function (success) {
                return typeof success !== 'undefined' && typeof success.data !== 'undefined' &&
                typeof success.data.count !== 'undefined' && success.data.count > 0 ? success.data.items : [];
            }

            var getActiveAlerts = function() {
                self.loadingAlerts = true;
                alertsService.alerts.getAll().then(function (success) {
                    self.alerts = getResult(success);
                    self.loadingAlerts = false;
                });
            }

            var getActiveRoutes = function() {
                self.loadingRoutes = true;
                alertsService.routes.getAll().then(function (success) {
                    self.routes = getResult(success);
                    self.loadingRoutes = false;
                });
            }

            var getServiceAlerts = function() {
                self.loadingServiceAlerts = true;
                alertsService.serviceAlerts.getAll().then(function (success) {
                    self.serviceAlerts = getResult(success);
                    self.loadingServiceAlerts = false;
                });

                alertsService.serviceAlerts.getValues().then(function (success) {
                    self.values = success.data;
                });
            }

            var getReceivers = function() {
                self.loadingReceivers = true;
                alertsService.receivers.getAll(true).then(function (success) {
                    self.receivers = getResult(success);
                    self.loadingReceivers = false;
                });
            }

            var getSilences = function() {
                self.loadingSilences = true;
                alertsService.silences.getAll().then(function (success) {
                    self.silences = getResult(success);
                    self.loadingSilences = false;
                });
            }

            var init = function () {
                getActiveAlerts();
                getServiceAlerts();
                getActiveRoutes();
                getReceivers();
                getSilences();
            }
            init();

            self.opened = function (name) {
                self.loadingReceiver = true;
                alertsService.receivers.get(name).then(function (success) {
                    self.receiver = success.data;
                    self.loadingReceiver = false;
                });
            };

            self.showReceiverValue = function (conf) {
                if (conf) {
                    return JSON.stringify(conf, null, 2);
                }
            };

            self.createServiceAlert = function () {
                alertsService.serviceAlerts.create(self.newAlert).then(function(success) {
                    growl.success(getMsg(success), {title: 'Alert Created', ttl: 1000});
                    getServiceAlerts();
                    getActiveRoutes();
                }, function(error){
                    growl.error(getMsg(error), {title: 'Failed to create alert', ttl: 5000});
                })
            };

            self.deleteServiceAlert = function(alert) {
                alertsService.serviceAlerts.delete(alert.id).then(function(success) {
                    growl.success(getMsg(success), {title: 'Alert deleted', ttl: 1000});
                    getServiceAlerts();
                    getActiveRoutes();
                }, function(error){
                    growl.error(getMsg(error), {title: 'Failed to delete alert', ttl: 5000});
                })
            };

            self.testServiceAlert = function(alert) {
                alertsService.serviceAlerts.test(alert.id).then(function(success) {
                    self.alerts = getResult(success);
                    growl.success(getMsg(success), {title: 'Alert sent', ttl: 1000});
                }, function(error){
                    growl.error(getMsg(error), {title: 'Failed to send alert', ttl: 5000});
                })
            };

            self.validReceiver = function (receivers) {
              if (typeof receivers !== 'undefined' && receivers.length > 0 && self.receivers.length > 0) {
                  var exist = false;
                  for (var i = 0; i < receivers.length; i++) {
                      for (var j=0; j<self.receivers.length; j++) {
                          if (receivers[i].name === self.receivers[j].name) {
                              exist = true;
                          }
                      }
                      if (exist) {
                         break;
                      }
                  }
                  return exist;
              } else {
                  return false;
              }
            };

            self.createSilence = function (form) {
                self.newSilence.endsAt = moment(self.newSilence.endsAt).format();
                self.newSilence.startsAt = moment(self.newSilence.startsAt).format();
                alertsService.silences.create(self.newSilence).then(function(success){
                    growl.success(getMsg(success), {title: 'Silence created.', ttl: 1000});
                    getSilences();
                    self.newSilence = {
                        comment: undefined,
                        startsAt: undefined,
                        endsAt: undefined,
                        matchers: [{isRegex: false, name: undefined, value: undefined}]
                    };
                    form.$setPristine();
                }, function(error){
                    growl.error(getMsg(error), {title: 'Failed to create silence.', ttl: 5000});
                })
            };

            var toKeyValArray = function(keyValStrArray) {
                if (typeof keyValStrArray === 'undefined' || keyValStrArray.length < 1) {
                    return undefined;
                }
                var keyValArray = [];
                for (var i = 0; i < keyValStrArray.length; i++) {
                    var keyVal = keyValStrArray[i].split('=');
                    keyValArray.push({key: keyVal[0], value: keyVal[1]});
                }
                return keyValArray;
            }

            self.createRoute = function(form) {
                var route = angular.copy(self.newRoute);
                route.match = toKeyValArray(route.match);
                route.matchRe = toKeyValArray(route.matchRe);
                alertsService.routes.create(route).then(function(success){
                    growl.success(getMsg(success), {title: 'Route created.', ttl: 1000});
                    getActiveRoutes();
                    self.newRoute = {
                        groupBy: undefined,
                        groupWait: undefined,
                        groupInterval: undefined,
                        repeatInterval: undefined,
                        receiver: undefined,
                        continue: false,
                        match: [],
                        matchRe: []
                    };
                    form.$setPristine();
                }, function(error){
                    growl.error(getMsg(error), {title: 'Failed to create route.', ttl: 5000});
                })
            }

            self.receiverConfigNewValue = function () {
                self.newReceiver.emailConfigs = undefined;
                self.newReceiver.slackConfigs = undefined;
                self.newReceiver.pagerdutyConfigs = undefined;
                self.newReceiver.pushoverConfigs = undefined;
                self.newReceiver.opsgenieConfigs = undefined;
                self.newReceiver.webhookConfigs = undefined;
                self.newReceiver.victoropsConfigs = undefined;
                self.newReceiver.wechatConfigs = undefined;
            }

            var toJson = function (receiver) {
                var newReceiver = angular.copy(receiver);
                newReceiver.emailConfigs = typeof newReceiver.emailConfigs !== 'undefined' ? JSON.parse(newReceiver.emailConfigs) : undefined;
                newReceiver.slackConfigs = typeof newReceiver.slackConfigs !== 'undefined' ? JSON.parse(newReceiver.slackConfigs) : undefined;
                newReceiver.pagerdutyConfigs = typeof newReceiver.pagerdutyConfigs !== 'undefined' ? JSON.parse(newReceiver.pagerdutyConfigs) : undefined;
                newReceiver.pushoverConfigs = typeof newReceiver.pushoverConfigs !== 'undefined' ? JSON.parse(newReceiver.pushoverConfigs) : undefined;
                newReceiver.opsgenieConfigs = typeof newReceiver.opsgenieConfigs !== 'undefined' ? JSON.parse(newReceiver.opsgenieConfigs) : undefined;
                newReceiver.webhookConfigs = typeof newReceiver.webhookConfigs !== 'undefined' ? JSON.parse(newReceiver.webhookConfigs) : undefined;
                newReceiver.victoropsConfigs = typeof newReceiver.victoropsConfigs !== 'undefined' ? JSON.parse(newReceiver.victoropsConfigs) : undefined;
                newReceiver.wechatConfigs = typeof newReceiver.wechatConfigs !== 'undefined' ? JSON.parse(newReceiver.wechatConfigs) : undefined;
                return newReceiver;
            };

            self.validateJSON = function(formController, value) {
                try {
                    JSON.parse(value);
                    formController.$setValidity('valid', true);
                } catch (e) {
                    formController.$setValidity('valid', false);
                }
                return true;
            }

            self.createReceiver = function(form) {
                var receiver = toJson(self.newReceiver);
                alertsService.receivers.create(receiver).then(function(success){
                    growl.success(getMsg(success), {title: 'Receiver created.', ttl: 1000});
                    getReceivers();
                    self.newReceiver = {
                        name: undefined,
                        emailConfigs: undefined,
                        slackConfigs: undefined,
                        pagerdutyConfigs: undefined,
                        pushoverConfigs: undefined,
                        opsgenieConfigs: undefined,
                        webhookConfigs: undefined,
                        victoropsConfigs: undefined,
                        wechatConfigs: undefined
                    };
                    form.$setPristine();
                }, function(error){
                    growl.error(getMsg(error), {title: 'Failed to create receiver.', ttl: 5000});
                });
            }

            self.isGlobalRoute = function (route) {
                if (typeof route === 'undefined' ||
                    typeof route.match === 'undefined' ||
                    typeof route.match['entry'] === 'undefined') {
                    return false;
                }
                var globalAlertTypes = angular.copy(self.values.alertType);
                globalAlertTypes.splice(globalAlertTypes.indexOf('PROJECT_ALERT'), 1);
                globalAlertTypes = globalAlertTypes.map(function(x){ return x.toLowerCase().replaceAll('_', '-'); })
                for (var i=0; i<route.match['entry'].length; i++) {
                    if (route.match['entry'][i].key === 'type' && globalAlertTypes.indexOf(route.match['entry'][i].value) > -1) {
                        return true;
                    }
                }
                return false;
            }

            self.isGlobalReceiver = function (receiver) {
                return receiver.name.startsWith(GLOBAL_RECEIVER);
            }

            self.deleteRoute = function (route) {
                if (typeof route === 'undefined') {
                    return false;
                }
                var match = typeof route.match === 'undefined' || typeof route.match['entry'] === 'undefined'? [] :
                    route.match['entry'].map(function(x){ return x.key + ':' + x.value; })
                var matchRe = typeof route.matchRe === 'undefined' || typeof route.matchRe['entry'] === 'undefined'? [] :
                    route.matchRe['entry'].map(function(x){ return x.key + ':' + x.value; })
                alertsService.routes.delete(route.receiver, match, matchRe).then(function(success){
                    growl.success(getMsg(success), {title: 'Route deleted.', ttl: 1000});
                    getActiveRoutes();
                }, function(error){
                    growl.error(getMsg(error), {title: 'Failed to delete route.', ttl: 5000});
                })
            }

            self.deleteReceiver = function (name) {
                alertsService.receivers.delete(name).then(function(success){
                    growl.success(getMsg(success), {title: 'Receiver deleted.', ttl: 1000});
                    getReceivers();
                }, function(error){
                    growl.error(getMsg(error), {title: 'Failed to delete receiver.', ttl: 5000});
                });
            }

            self.deleteSilence = function (id) {
                alertsService.silences.delete(id).then(function(success){
                    growl.success(getMsg(success), {title: 'Silence deleted.', ttl: 1000});
                    getSilences();
                }, function(error){
                    growl.error(getMsg(error), {title: 'Failed to delete silence.', ttl: 5000});
                })
            }

        }]);