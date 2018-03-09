'use strict';

angular.module('hopsWorksApp')
        .controller('DevicesSettingsCtrl', ['DeviceManagementService', '$routeParams', 'growl', 
          function (DeviceManagementService,  $routeParams, growl) {

            var self = this;
            self.working = false;
            self.projectId = $routeParams.projectID;
            self.enabled;
            self.jwtTokenDurationInHours;

            self.enabledStates = [
              {"name": "Disabled", "value": 0},
              {"name": "Enabled", "value": 1}, 
            ]

            self.jwtTokenDurationInHoursStates = [
              {"name": "1 hour", "value": 1},
              {"name": "1 day", "value": 24},
              {"name": "7 days", "value": 168},
              {"name": "30 days", "value": 720},
              {"name": "365 days", "value": 8760},
            ]


            self.getDevicesSettings = function () {
              DeviceManagementService.getDevicesSettings(self.projectId).then(
                      function (response) {
                        var devicesSettings = response.data;
                        self.enabled = self.enabledStates.filter(function(item) {
                            return item.value === devicesSettings.enabled;
                        })[0];
                        self.jwtTokenDurationInHours = self.jwtTokenDurationInHoursStates.filter(function(item) {
                            return item.value === devicesSettings.jwtTokenDurationInHours;
                        })[0];
                      }, function (error) {
                        growl.error(error.errorMsg, {title: 'Error', ttl: 2000});
                      });
            };

            self.getDevicesSettings();

            self.postDevicesSettings = function () {
              self.working = true;
              var devicesSettings = {}
              devicesSettings.enabled = self.enabled.value;
              devicesSettings.jwtTokenDurationInHours = self.jwtTokenDurationInHours.value;
              DeviceManagementService.postDevicesSettings(self.projectId, devicesSettings).then(
                      function (success) {
                        self.working = false;
                        growl.success("The device settings has been saved successfully.", {title: 'Device settings saved', ttl: 2000});
                      }, function (error) {
                        self.working = false;
                        growl.error(error.errorMsg, {title: 'Error', ttl: 2000});
                      });
            };

          }]);
