/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('KafkaCtrl', ['$scope', '$routeParams', 'growl', 'KafkaService', '$location', 'ModalService', '$interval', '$mdSidenav',
          function ($scope, $routeParams, growl, KafkaService, $location, ModalService, $interval, $mdSidenav) {

            var self = this;
            self.projectId = $routeParams.projectID;
            self.topics = [{"name": "myTopic", 'acls': [{'id': '4563', 'username': "bbb", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}, {'id': '999', 'username': "hdhd", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}]},
              {"name": "yourTopic", 'acls': [{'id': '443', 'username': "aaa", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}]},
              {"name": "thisTopic", 'acls': [{'id': '123', 'username': "ccc", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}]}];

            self.numTopicsLeft = 0;

            self.currentTopic = null;
            self.topicName = "";
            self.username = "";
            self.permission_type = "";
            self.operation_type = "";
            self.host = "*";
            self.role = "*";
            self.shared = "*";
            self.activeId = -1;

            self.selectAcl = function (acl) {
              if (self.activeId === acl.id) { //unselect the current selected ACL
                self.activeId = 0;
                return;
              }
              self.username = acl.username;
              self.permission_type = acl.permission_type;
              self.operation_type = acl.operation_type;
              self.host = acl.host;
              self.role = acl.role;
              self.shared = acl.shared;
              self.activeId = acl.id;
            };


            self.getAllTopics = function () {
              KafkaService.getTopics(self.projectId).then(
                      function (success) {
//                        self.topics = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

//            self.getAllTopics();


            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.createTopic = function () {
              KafkaService.createTopic(self.projectId, self.topicName).then(
                      function (success) {
                        self.getAllTopics();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
              });
            };

            self.removeTopic = function (topicName, topicId) {
              ModalService.confirm("sm", "Delete Topic (" + topicName + ")",
                      "Do you really want to delete this topic? This action cannot be undone.")
                      .then(function (success) {
                        KafkaService.removeTopic(self.projectId, topicId).then(
                                function (success) {
                                  getTopics();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 5000});
                        });
                      }, function (cancelled) {
                        growl.info("Delete aborted", {title: 'Info', ttl: 2000});
                      });
            };


            self.getAclsForTopic = function (topicId) {
              KafkaService.getAclsForTopic(self.projectId, topicId).then(
                      function (success) {
                        self.topics[topicId].acls = success.data;
                        self.activeId = "";
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to get ACLs for the topic', ttl: 5000});
              });
            };

            self.addAcl = function (topicId) {
              var acl = {};
              acl.role = "*";
              acl.topicId = topicId;
              acl.username = self.username;
              acl.permission_type = self.permission_type;
              acl.operation_type = self.operation_type;
              acl.host = self.host;
              acl.shared = self.shared;

              KafkaService.addAcl(self.projectId, topicId, acl).then(
                      function (success) {
                        self.getAclsForTopic();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 5000});
              });

            };

            self.removeAcl = function (topicId, aclId) {
              KafkaService.removeTopicAcl(self.projectId, topicId, aclId).then(
                      function (success) {
                        self.getAclsForTopic();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 5000});
              });

            };

          }]);



