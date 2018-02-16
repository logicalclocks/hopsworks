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

