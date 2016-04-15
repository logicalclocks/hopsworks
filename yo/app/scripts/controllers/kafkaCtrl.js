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

            getAllTopics();


            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.createTopic = function () {
              KafkaService.createTopic(self.projectId, topicName).then(
                      function (success) {
                        getTopics();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
              });


//              StorageService.clear();
//              $location.path('project/' + self.projectId + '/newjob');
            };

            self.showTopicDetails = function (topic) {
              KafkaService.getTopicDetails  (self.projectId, topicName).then(
                      function (success) {
                        getTopics();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to get topic details', ttl: 5000});
              });
            };


            self.removeTopic = function (topicName) {
              ModalService.confirm("sm", "Delete Topic (" + topicName + ")",
                      "Do you really want to delete this topic?\n\
                                This action cannot be undone.")
                      .then(function (success) {
                        KafkaService.removeTopic(self.projectId, topicName).then(
                                function (success) {
                                  getTopics();
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 5000});
                        });
                      }, function (cancelled) {
                        growl.info("Delete aborted", {title: 'Info', ttl: 2000});
                      });
            };



          }]);



