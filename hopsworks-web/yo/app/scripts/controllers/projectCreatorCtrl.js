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
            self.card = {};
            self.myCard = {};
            self.cards = [];
            self.user = {
              firstName: '',
              lastName: '',
              email: '',
              telephoneNum: '',
              registeredon: '',
              twoFactor: ''
            };
            
            self.projectMembers = [];
            self.projectTeam = [];
            if ($rootScope.isDelaEnabled) {
              // , 'RSTUDIO'
              self.projectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'DELA', 'SERVING'];
              self.selectionProjectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'DELA', 'SERVING'];
            } else {
              self.projectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'SERVING'];
              self.selectionProjectTypes = ['JOBS', 'KAFKA', 'JUPYTER', 'HIVE', 'SERVING'];
            }

            self.projectName = '';
            self.projectDesc = '';

            self.regex = /^[a-zA-Z0-9]((?!__)[_a-zA-Z0-9]){0,62}$/;

            UserService.profile().then(
                    function (success) {
                      self.user = success.data;
                      if (success.data.email !== undefined) {
                        self.myCard.email = success.data.email;
                        if (success.data.firstName !== undefined) {
                          self.myCard.firstname = success.data.firstName;
                          if (success.data.email !== undefined) {
                            self.myCard.lastname = success.data.lastName;
                            UserService.allcards().then(
                                    function (success) {
                                      self.cards = success.data;
                                      // remove my own 'card' from the list of members
                                      for (var i = 0, len = self.cards.length; i < len; i++) {
                                        if (self.cards[i].email === self.myCard.email) {
                                          self.cards.splice(i, 1);
                                          break;
                                        }
                                      }
                                      for (var i = 0, len = self.cards.length; i < len; i++) {
                                        if (self.cards[i].email === "agent@hops.io") {
                                          self.cards.splice(i, 1);
                                          break;
                                        }
                                      }
                                    }, function (error) {
                              self.errorMsg = error.data.msg;
                            });
                          }
                        }
                      }

                    },
                    function (error) {
                      self.errorMsg = error.data.errorMsg;
                    });


            $scope.$watch('projectCreatorCtrl.card.selected', function (selected) {
              var projectTeamPK = {'projectId': "", 'teamMember': ""};
              var projectTeam = {'projectTeamPK': projectTeamPK};
              if (selected !== undefined) {
                projectTeamPK.teamMember = selected.email;
                if (self.projectMembers.indexOf(selected.email) === -1) {
                  self.projectMembers.push(selected.email);
                  self.projectTeam.push(projectTeam);
                }
                self.card.selected = undefined;
              }
            });


            self.addSelected = function exists(projectType) {
              var idx = self.selectionProjectTypes.indexOf(projectType);
              if (idx > -1) {
                self.selectionProjectTypes.splice(idx, 1);
              } else {
                self.selectionProjectTypes.push(projectType);
              }
            };

            self.exists = function exists(projectType) {
              var idx = self.selectionProjectTypes.indexOf(projectType);
              return idx > -1;
            };


            self.removeMember = function (member) {
              self.projectMembers.splice(self.projectMembers.indexOf(member), 1);
            };

            self.createProject = function () {
              self.working = true;
              $scope.newProject = {
                'projectName': self.projectName,
                'description': self.projectDesc,
                'retentionPeriod': "",
                'status': 0,
                'services': self.selectionProjectTypes,
                'projectTeam': self.projectTeam
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

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

          }]);
