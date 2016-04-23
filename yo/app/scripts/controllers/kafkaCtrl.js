/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('KafkaCtrl', ['$scope', '$routeParams', 'growl', 'KafkaService', '$location', 'ModalService', '$interval', '$mdSidenav',
          function ($scope, $routeParams, growl, KafkaService, $location, ModalService, $interval, $mdSidenav) {

            var self = this;
            self.projectId = $routeParams.projectID;
            self.topics = [{name : "myTopic", acl :
                [{ username: "bbb", permission_type: "write", operation_type: "write", host: "*", role: "*", shared: "*"}] }
              ]; 

            self.currentTopic = null;
            self.topicName = "";
            self.username = "";
            self.permission_type = "";
            self.operation_type = "";
            self.host = "*";
            self.role = "*";
            self.shared = "*";

            var getAllTopics = function () {
              KafkaService.getTopics(self.projectId).then(
                      function (success) {
                        self.topics = success.data;
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

            self.removeTopic = function (topicId) {
              ModalService.confirm("sm", "Delete Topic (" + topicName + ")",
                      "Do you really want to delete this topic?\n\
                                This action cannot be undone.")
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
                        topics[topicId] = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to get topic details', ttl: 5000});
              });
            };

            self.addAcl = function (topicId, acl) {
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



