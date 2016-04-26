/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('KafkaCtrl', ['$scope', '$routeParams', 'growl', 'KafkaService', '$location', 'ModalService', '$interval', '$mdSidenav',
          function ($scope, $routeParams, growl, KafkaService, $location, ModalService, $interval, $mdSidenav) {

            var self = this;
            self.projectId = $routeParams.projectID;
            self.topics = [{"name": "myTopic", 
                'acls': [{'id': '4563', 'username': "bbb", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}, {'id': '999', 'username': "hdhd", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}],
                'shares': [{'proj_name': 'shareOne'},{'proj_name': 'shareTwo'}]              
                 },
              {"name": "yourTopic", 'acls': [{'id': '443', 'username': "aaa", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}], 'shares': []},
              {"name": "thisTopic", 'acls': [{'id': '123', 'username': "ccc", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}], 'shares': []},
              {"name": "emptyTopic", 'acls': [], 'shares': []}
            ];

//            self.topicDetails = {"name": "myTopic", "partitionReplicas" : [{"id" : "21212", "partitionEndpoints" : []}]
//                , "partitionLeader": [{"id": "234545", "leader" : "1.2.3.5:8484"}]
//                , "inSyncReplicas" : [{"id" : "11112", "partitionEndpoints" : ["4.2.3.5:8484", "2.2.3.5:5555"]}, 
//                    {"id" : "1666112", "partitionEndpoints" : []}]
//            }; 


            self.topicDetails = {"name": "myTopic", 
                                 "partitionDetails" : [{ "id" : "21212",
                                            "paritionLeader": "1.2.3.4:8484",
                                            "partitionReplicas" : ["4.2.3.5:8484", "2.2.3.5:5555", "1.2.3.4:8484"],
                                            "paritionInSyncReplicas": ["4.2.3.5:8484", "1.2.3.4:8484"]}]
            }; 

            self.maxNumTopics = 10;
            self.numTopicsUsed = 0;

            self.currentTopic = "";
            self.topicName = "";
            self.numReplicas = "";
            self.numPartitions = "";
            self.username = "";
            self.permission_type = "";
            self.operation_type = "";
            self.host = "*";
            self.role = "*";
            self.shared = "*";
            self.activeId = -1;
            self.activeShare = -1;

            self.selectAcl = function (acl) {
              if (self.activeId === acl.id) { //unselect the current selected ACL
//                self.activeId = -1;
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
              KafkaService.createTopic(self.projectId, self.topicDetails).then(
                      function (success) {
                        self.getAllTopics();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
              });
            };

            self.removeTopic = function (topicName) {
              ModalService.confirm("sm", "Delete Topic (" + topicName + ")",
                      "Do you really want to delete this topic? This action cannot be undone.")
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


            self.getAclsForTopic = function (topicName) {
              KafkaService.getAclsForTopic(self.projectId, topicName).then(
                      function (success) {
                        self.topics[topicName].acls = success.data;
                        self.activeId = "";
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to get ACLs for the topic', ttl: 5000});
              });
            };

            self.addAcl = function (topicName) {
              var acl = {};
              acl.role = "*";
              acl.topicName = topicName;
              acl.username = self.username;
              acl.permission_type = self.permission_type;
              acl.operation_type = self.operation_type;
              acl.host = self.host;
              acl.shared = self.shared;

              KafkaService.addAcl(self.projectId, topicName, acl).then(
                      function (success) {
                        self.getAclsForTopic();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 5000});
              });

            };

            self.removeAcl = function (topicName, aclId) {
              KafkaService.removeTopicAcl(self.projectId, topicName, aclId).then(
                      function (success) {
                        self.getAclsForTopic();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 5000});
              });

            };
            
            
            self.shareTopic = function(topicName) {
              ModalService.selectProject('lg', "/[^]*/",
                      "Select a Project to share the topic with.").then(
                      function (success) {
                        var destProj = success;

                        KafkaService.shareTopic(self.projectId, topicName, destProj.projectId).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'Topic shared successfully with project: ' + destProj.name, ttl: 2000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });


                      }, function (error) {
                //The user changed their mind.
              });

            };
              
            self.unshareTopic = function(topicName, projectName) {
              ModalService.selectProject('lg', "/[^]*/",
                      "problem selecting project").then(
                      function (success) {
                        var destProj = success;

                        KafkaService.unshareTopic(self.projectId, topicName, projectName).then(
                                function (success) {
                                  growl.success(success.data.successMessage, {title: 'Topic share removed (unshared) rom project: ' + destProj.name, ttl: 2000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });


                      }, function (error) {
                //The user changed their mind.
              });

            };
              

          }]);



