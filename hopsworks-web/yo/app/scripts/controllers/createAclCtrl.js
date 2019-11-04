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

angular.module('hopsWorksApp')
        .controller('CreateAclCtrl', ['$uibModalInstance', 'KafkaService', 'growl', 'projectId', 'topicName',
          'projectName', 'MembersService',
          function ($uibModalInstance, KafkaService, growl, projectId, topicName, projectName, MembersService) {

            var self = this;
            self.users = [];
            self.projectId = projectId;
            self.topicName = topicName;
            self.projectName = projectName;
            self.permission_type;

            self.project;
            self.projects = [];
            self.permission_type = "Allow";
            self.operation_type = "Read";
            self.host = "*";
            self.role = "*";


            self.init = function () {
              self.projects = self.getProjectsForTopic(self.projectId, self.topicName);
            };

              self.getProjectsForTopic = function (projectId, topicName) {
                  KafkaService.topicIsSharedTo(projectId, topicName).then(
                    function (success) {
                        var res = success.data.items != null ? success.data.items : [];
                        var project = {};
                        project.id = projectId;
                        project.name = self.projectName;
                        res.push(project);
                        self.projects = res;
                    }
                  )
              };

            self.getAclUsersForProject = function (item) {
              MembersService.query({id: item.id}).$promise.then(
                function (success) {
                  var emails = success.map(function(item) {
                      return item.user.email;
                  });
                  emails.push("*");
                  self.users = emails;
                },
                function (error) {
                }
              );
            };

            self.init();

            self.createTopicAcl = function () {

              var aclArray = [];
              var emailsArray = [];
              if (self.userEmail === '*') {
                emailsArray = self.users
                  .filter(function(email) {
                    return email !== '*';});
              } else {
                emailsArray.push(self.userEmail);
              }
              emailsArray.forEach(function (email) {
                var acl = {};
                acl.projectName = self.project.name;
                acl.role = self.role;
                acl.userEmail = email;
                acl.permissionType = self.permission_type;
                acl.operationType = self.operation_type;
                acl.host = self.host;
                aclArray.push(acl);
              });
              aclArray.forEach(self.sendCreateTopicAcl);
            };

            self.sendCreateTopicAcl = function (acl) {
                KafkaService.createTopicAcl(self.projectId, topicName, acl).then(
                    function (success) {
                        $uibModalInstance.close(success);
                    }, function (error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                        }
                    });
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

