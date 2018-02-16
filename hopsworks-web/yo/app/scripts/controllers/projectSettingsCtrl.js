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