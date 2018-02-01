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

angular.module('hopsWorksApp')
        .controller('CreateAclCtrl', ['$uibModalInstance', 'KafkaService', 'growl', 'projectId', 'topicName',
          function ($uibModalInstance, KafkaService, growl, projectId, topicName) {

            var self = this;
            self.users = [];
            self.projectId = projectId;
            self.topicName = topicName;
            self.permission_type;

            self.project;
            self.user_email = "admin@kth.se";
            self.permission_type = "Allow";
            self.operation_type = "Read";
            self.host = "*";
            self.role = "*";


            self.init = function () {
              KafkaService.aclUsers(self.projectId, self.topicName).then(
                      function (success) {
                        self.users = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Could not load ACL users', ttl: 5000, referenceId: 21});
              });
            };

            self.init();

            self.createTopicAcl = function () {

              var acl = {};
              acl.projectName = self.project.projectName;
              acl.role = self.role;
              acl.userEmail = self.userEmail;
              acl.permissionType = self.permission_type;
              acl.operationType = self.operation_type;
              acl.host = self.host;

              KafkaService.createTopicAcl(self.projectId, topicName, acl).then(
                      function (success) {
                        $uibModalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Could not create ACL', ttl: 5000, referenceId: 21});
              });
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

