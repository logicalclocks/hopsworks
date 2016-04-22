/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('KafkaCtrl', ['$scope', '$routeParams', 'growl', 'KafkaService', '$location', 'ModalService', '$interval', 'StorageService', '$mdSidenav',
          function ($scope, $routeParams, growl, KafkaService, $location, ModalService, $interval, StorageService, $mdSidenav) {

            var self = this;
            this.projectId = $routeParams.projectID;
            this.topics []; // Will contain all the jobs.

            self.currentTopic = null;

            var getAllTopics = function () {
              KafkaService.getTopics(self.projectId).then(
                      function (success) {
                        self.topics = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

            self.getAllTopics();


            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.createTopic = function (topicName) {
              KafkaService.createTopic(self.projectId, topicName).then(
                      function (success) {
                       self.getAllTopics();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
              });
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


//              StorageService.clear();
//              $location.path('project/' + self.projectId + '/newjob');
            };

            self.getAclsForTopic = function (topicId) {
              KafkaService.getAclsForTopic  (self.projectId, topicId).then(
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



