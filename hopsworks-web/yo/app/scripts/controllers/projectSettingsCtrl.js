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

/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .controller('ProjectSettingsCtrl', ['ProjectService', '$routeParams', '$location', 'growl', 'VariablesService',
          function (ProjectService,  $routeParams, $location, growl, VariablesService) {

            var self = this;
            self.projectId = $routeParams.projectID;
            self.quotas = {};
            self.versions = [];
            self.pia = {
              "id": "",
              "personalData": "",
              "howDataCollected": "",
              "specifiedExplicitLegitimate": 0,
              "consentProcess": "",
              "consentBasis": "",
              "dataMinimized": 0,
              "dataUptodate": 0,
              "usersInformed_how": "",
              "userControlsDataCollectionRetention": "",
              "dataEncrypted": 0,
              "dataAnonymized": 0,
              "dataPseudonymized": 0,
              "dataBackedup": 0,
              "dataSecurityMeasures": "",
              "dataPortabilityMeasure": "",
              "subjectAccessRights": "",
              "risks": ""
            };
            var getPia = function () {
              ProjectService.getPia({id: self.projectId}).$promise.then(
                function (success) {
                  self.pia = success;
                }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error getting Pia', ttl: 5000});
                  $location.path('/');
              });
            };
            getPia();
            
            self.savePia = function () {
              var forms = document.getElementsByClassName('needs-validation');
              Array.prototype.filter.call(forms, function (form) {
                form.addEventListener('submit', function (event) {
                  if (form.checkValidity() === false) {
                    event.preventDefault();
                    event.stopPropagation();
                  }
                  form.classList.add('was-validated');
                }, false);
              });

              ProjectService.savePia({id: self.projectId}, self.pia)
                .$promise.then(function (success) {
                    growl.success("Saved Pia", {title: 'Saved', ttl: 2000});
                  }, function (error) {
                    self.working = false;
                    growl.warning("Error: " + error.data.errorMsg, {title: 'Error', ttl: 5000});
                });
            };


            self.getQuotas = function () {
              ProjectService.getQuotas({id: self.projectId}).$promise.then(
                      function (response) {
                        self.quotas = response;
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000});
                      }
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
            
            var getVersions = function () {
              if (self.versions.length === 0) {
                VariablesService.getVersions()
                  .then(function (success) {
                    self.versions = success.data;
                  }, function (error) {
                    console.log("Failed to get versions");
                });
              }
            };
            getVersions();


          }]);