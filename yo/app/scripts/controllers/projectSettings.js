/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ProjectSettingsCtrl', ['$modalInstance', '$scope', 'ProjectService', 'growl', 'projectId',
          function ($modalInstance, $scope, ProjectService, growl, projectId) {

            var self = this;
            self.projectId = projectId;
            self.quotas = {};

            self.getQuotas = function () {
              ProjectService.getQuotas({id: self.projectId}).$promise.then(
                      function (response) {
                        self.quotas = response;
                      }, function (error) {
                growl.error(error.errorMsg, {title: 'Error', ttl: 2000});
              });

            };

            self.getQuotas();

            self.hdfsUsage = function (id) {
                return convertSize(self.quotas.hdfsUsageInBytes);
            };

            self.hdfsQuota = function (id) {
                return convertSize(self.quotas.hdfsQuotaInBytes);
            };

            self.hdfsNsCount = function (id) {
                return convertNs(self.quotas.hdfsNsCount);
            };

            self.hdfsNsQuota = function (id) {
                return convertNs(self.quotas.hdfsNsQuota);
            };

            self.yarnQuota = function (id) {
                return convertSeconds(self.quotas.yarnQuotaInMins);
            };

            self.ok = function () {
              $modalInstance.close();
            };

          }]);