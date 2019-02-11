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

angular.module('hopsWorksApp')
        .controller('ProjectCreatorCtrl', ['$uibModalInstance', '$scope', '$rootScope', 'ProjectService', 'UserService', 'growl',
          function ($uibModalInstance, $scope, $rootScope, ProjectService, UserService, growl) {

            var self = this;

            self.working = false;
            self.loadingUsers = false;
            self.users = [];
            self.selectedUsers = [];
            self.selectNoChoice = 'Could not find any user...';
            self.user = {
              firstname: '',
              lastname: '',
              email: ''
            };

            self.projectTeam = [];
            if ($rootScope.isDelaEnabled) {
              // , 'RSTUDIO'
              self.projectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'DELA', 'SERVING', 'FEATURESTORE', 'AIRFLOW'];
              self.selectionProjectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'DELA', 'SERVING', 'FEATURESTORE', 'AIRFLOW'];
            } else {
		self.projectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'SERVING', 'FEATURESTORE', 'AIRFLOW'];
		self.selectionProjectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'SERVING', 'FEATURESTORE', 'AIRFLOW'];
            }

            self.projectName = '';
            self.projectDesc = '';

            self.regex = /^[a-zA-Z0-9]((?!__)[_a-zA-Z0-9]){0,62}$/;
            
            var inSelectedUsers = function (email) {
              var len = self.selectedUsers.length;
              for (var i = 0; i < len; i++) {
                if (self.selectedUsers[i].email === email) {
                  return true;
                }
              }
              return false;
            };
            
            var getUsers = function (query) {
              self.loadingUsers = true;
              UserService.profile().then(               
                function (success) {
                  self.user = success.data;
                  UserService.allValidUsers(query).then(function (success) {
                      var items = success.data.items;
                      var countRemoved = 0;
                      for (var i = items.length -1; i >= 0; i--) {
                        if (items[i].email === self.user.email || inSelectedUsers(items[i].email)) {
                          items.splice(i, 1);
                        }
                        countRemoved++;
                        if(countRemoved === self.selectedUsers.length + 1){
                          break;
                        }
                      }
                      self.users = items;
                      self.loadingUsers = false;
                    }, function (error) {
                      self.errorMsg = error.data.msg;
                      self.loadingUsers = false;
                  });
                },
                function (error) {
                  self.errorMsg = error.data.errorMsg;
                  self.loadingUsers = false;
              });
            };

            self.addSelected = function (projectType) {
              var idx = self.selectionProjectTypes.indexOf(projectType);
              if (idx > -1) {
                self.selectionProjectTypes.splice(idx, 1);
              } else {
                self.selectionProjectTypes.push(projectType);
              }
            };

            self.exists = function (projectType) {
              var idx = self.selectionProjectTypes.indexOf(projectType);
              return idx > -1;
            };
            
            var getProjectTeams = function () {
              self.selectedUsers.forEach(function (selected) {
                var projectTeamPK = {'projectId': "", 'teamMember': ""};
                var projectTeam = {'projectTeamPK': projectTeamPK};
                projectTeamPK.teamMember = selected.email;
                self.projectTeam.push(projectTeam);
              });
              return self.projectTeam;
            };

            self.createProject = function () {
              self.working = true;
              $scope.newProject = {
                'projectName': self.projectName,
                'description': self.projectDesc,
                'retentionPeriod': "",
                'status': 0,
                'services': self.selectionProjectTypes,
                'projectTeam': getProjectTeams()
              };
              ProjectService.save($scope.newProject).$promise.then(
                      function (success) {
                        self.working = false;
                        growl.success(success.successMessage, {title: 'Success', ttl: 2000});
                        if (success.errorMsg) {
                          growl.warning(success.errorMsg, {title: 'Error', ttl: 10000});
                        }
                        if (typeof success.fieldError !== 'undefined' && success.fieldErrors.length > 0) {
                          success.fieldErrors.forEach(function (entry) {
                            growl.warning(entry + ' could not be added', {title: 'Error', ttl: 10000});
                          });
                        }
                        $uibModalInstance.close($scope.newProject);
                      }, function (error) {
                      self.working = false;
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 1});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000, referenceId: 1});

                      }
              });
            };
            
            self.fetchAsync = function (query) {
              if (query) {
                getUsers(query);
                self.selectNoChoice = 'Could not find any user...';
              } else {
                self.users = undefined;
                self.selectNoChoice = 'Search for a user...';
              }
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

          }]);
