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

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ProjectSettingsCtrl', ['ProjectService', '$routeParams', 'growl', 
          function (ProjectService,  $routeParams, growl) {

            var self = this;
            self.projectId = $routeParams.projectID;
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
              if (self.quotas === {} ||  self.quotas.yarnQuotaInMins === undefined ) {
                return 'unknown';
              }
              var quota = self.quotas.yarnQuotaInMins;
              var pos = quota.lastIndexOf('.');
              var quotaInt = quota.substring(0, pos);

              return convertSeconds(parseInt(quotaInt));
            };


          }]);