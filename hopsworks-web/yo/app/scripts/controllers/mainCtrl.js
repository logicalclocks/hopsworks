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
        .controller('MainCtrl', ['$q', '$interval', '$location', '$scope', '$rootScope',
          '$http', 'AuthService', 'UtilsService', 'ElasticService', 'DelaProjectService',
          'DelaService', 'md5', 'ModalService', 'ProjectService', 'growl',
          'MessageService', '$routeParams', '$window', 'HopssiteService', 'BannerService',
          'AirflowService', 'PaginationService', 'VariablesService', 'StorageService',
          function ($q, $interval, $location, $scope, $rootScope, $http, AuthService, UtilsService,
                  ElasticService, DelaProjectService, DelaService, md5, ModalService, 
                  ProjectService, growl,
                  MessageService, $routeParams, $window, HopssiteService, BannerService,
                  AirflowService, PaginationService, VariablesService, StorageService) {
            var self = this;

            const MAX_IN_MEMORY_ITEMS = 1000;
            self.ui = "/hopsworks-api/airflow/login?q=username=";

            self.email = StorageService.get('email');
            self.emailHash = md5.createHash(self.email || '');

            var checkeIsAdmin = function () {
            var isAdmin = sessionStorage.getItem("isAdmin");
            if (isAdmin != 'true' && isAdmin != 'false') {
                AuthService.isAdmin().then(
                  function (success) {
                    sessionStorage.setItem("isAdmin", success.data.data);
                  }, function (error) {
                    sessionStorage.setItem("isAdmin", null);
                });
              }
            };
            checkeIsAdmin();
            self.isAdmin = function () {
              return sessionStorage.getItem("isAdmin");
            };

            self.goToAdminPage = function () {
              $window.location.href = '/hopsworks-admin/security/protected/admin/adminIndex.xhtml';
            };

            self.getEmailHash = function (email) {
              return md5.createHash(email || '');
            };

            self.logout = function () {
              AirflowService.logout();

              AuthService.logout(self.user).then(
                function (success) {
                    AuthService.cleanSession();
                    AuthService.removeToken();
                    $location.url('/login');
                }, function (error) {
                    self.errorMessage = error.data.msg;
                });
            };

            var checkDelaEnabled = function () {
              
              HopssiteService.getServiceInfo("dela").then(function (success) {
                self.delaServiceInfo = success.data;
                if (self.delaServiceInfo.status === 1) {
                  $rootScope['isDelaEnabled'] = true;
                } else {
                  $rootScope['isDelaEnabled'] = false;
                }
              }, function (error) {
                $rootScope['isDelaEnabled'] = false;
                console.log("isDelaEnabled", error);
              });
            };
            //checkDelaEnabled(); // check 
            self.userNotification = '';
            var getUserNotification = function () {
              self.userNotification = '';
              BannerService.findUserBanner().then(
                      function (success) {
                        if (success.data.successMessage) {
                          self.userNotification = success.data.successMessage;
                        }
                      }, function (error) {
                        console.log(error);
                        self.userNotification = '';
              });
            };
            getUserNotification();

            self.profileModal = function () {
                $location.url('/settings?tab=credentials');
            };

            self.sshKeysModal = function () {
              ModalService.sshKeys('lg');
            };

            self.getHostname = function () {
              return $location.host();
            };

            self.getUser = function () {
              return self.email.substring(0, self.email.indexOf("@"));
            };

            var getUnreadCount = function () {
              MessageService.getUnreadCount().then(
                      function (success) {
                        self.unreadMessages = success.data.data.value;
                      }, function (error) {
              });
            };

            var getMessages = function () {
              MessageService.getMessages().then(
                      function (success) {
                        self.messages = success.data;
                      }, function (error) {
              });
            };

            getUnreadCount();
            getMessages();

            var getUnreadCountInterval = $interval(function () {
              getUnreadCount();
            }, 10000);

            self.getMessages = function () {
              getMessages();
            };

            self.openMessageModal = function (selected) {
              if (selected !== undefined) {
                MessageService.markAsRead(selected.id);
              };
              ModalService.messages('lg', selected)
                      .then(function (success) {
                        growl.success(success.data.successMessage,
                                {title: 'Success', ttl: 1000});
                      }, function (error) {
                      });
            };

            self.searchTerm = "";
            const searchScopes = {
              "datasetCentric": ['This dataset'],
              "projectCentric": ['This project', 'Datasets', 'Feature store'],
              "global": ['Everything', 'Projects', 'Datasets', 'Feature store']
            };

            if (!angular.isUndefined($routeParams.datasetName)) {
              self.searchType = "datasetCentric";
            } else if (!angular.isUndefined($routeParams.projectID)) {
              self.searchType = "projectCentric";
            } else {
              self.searchType = "global";
            }
            self.searchScopes = searchScopes[self.searchType];
            self.searchScope = self.searchScopes[0];

            self.searchInScope = function (scope, searchTerm) {
              self.searchScope = scope;
              self.searchTerm = searchTerm;
              if (self.searchTerm === undefined || self.searchTerm === "" || self.searchTerm === null) {
                  return;
              }
              var searchPath = '/search';
              if (!angular.isUndefined($routeParams.projectID)) {
                  searchPath = '/project/' + $routeParams.projectID + '/search';
              }
              if (!angular.isUndefined($routeParams.datasetName)) {
                  searchPath = '/project/' + $routeParams.projectID + '/datasets/' + $routeParams.datasetName + '/search';
              }
              $location.path(searchPath);
              $location.search('scope', self.searchScope);
              $location.search('q', self.searchTerm);
            };

            $scope.$on("$destroy", function () {
              $interval.cancel(getUnreadCountInterval);
              //$interval.cancel(getPopularPublicDatasetsInterval);
            });

            //TODO: move to search
            self.downloadPublicDataset = function (result) {
              ModalService.selectProject('md', true, "/[^]*/", "Select a Project as download destination.", true).then(function (success) {
                var destProj = success.projectId;
                ModalService.setupDownload('md', destProj, result).then(function (success) {
                }, function (error) {
                  if (error.data && error.data.details) {
                    growl.error(error.data.details, {title: 'Error', ttl: 1000});
                  }
                  self.delaHopsService = new DelaProjectService(destProj);
                  self.delaHopsService.unshareFromHops(result.publicId, true).then(function (success) {
                    growl.info("Download cancelled.", {title: 'Info', ttl: 1000});
                  }, function (error) {
                    growl.warning(error, {title: 'Warning', ttl: 1000});
                  });
                });
              }, function (error) {
              });
            };

            self.versions = [];
            self.hopsworksDocVersion = undefined;
            $rootScope.hopsworksDocVersion = undefined;
            var getHopsworksVersion = function (versions) {
              for (var i = 0; i < versions.length; i++) {
                  if (versions[i].software === 'hopsworks') {
                      if (versions[i].version.endsWith('SNAPSHOT')) {
                          self.hopsworksDocVersion = 'latest';
                      } else {
                          var index = versions[i].version.lastIndexOf('.');
                          self.hopsworksDocVersion = versions[i].version.substring(0, index);
                      }
                      $rootScope.hopsworksDocVersion = self.hopsworksDocVersion;
                  }
              }
            }

            var getVersions = function () {
              if (self.versions.length === 0) {
                  VariablesService.getVersions()
                      .then(function (success) {
                          self.versions = success.data;
                          getHopsworksVersion(self.versions);
                      }, function (error) {
                          console.log("Failed to get versions");
                      });
              }
            };
            getVersions();

            self.ShowMetadataDesigner = true;

            var getMetadataDesignerVar = function () {
                VariablesService.getVariable('enable_metadata_designer')
                    .then(function (success) {
                        self.ShowMetadataDesigner = success.data.successMessage == 'true';
                    }, function (error) {
                        console.log("Failed to get enable_metadata_designer");
                    });
            };
            getMetadataDesignerVar();

          }]);
