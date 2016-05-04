/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('KafkaCtrl', ['$scope', '$routeParams', 'growl', 'KafkaService', '$location', 'ModalService', '$interval', '$mdSidenav',
          function ($scope, $routeParams, growl, KafkaService, $location, ModalService, $interval, $mdSidenav) {

            var self = this;
            self.projectId = $routeParams.projectID;
            self.topics = 
                    [{"name": "myTopic", 
                'acls': [{'id': '4563', 'username': "bbb", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}, {'id': '999', 'username': "hdhd", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}],
                'shares': [{'proj_name': 'shareOne'},{'proj_name': 'shareTwo'}],
                'partitionDetails': [{ 'id' : "21212",
                                            'partitionLeader': "1.2.3.4:8484",
                                            'partitionReplicas' : ["4.2.3.5:8484", "2.2.3.5:5555", "1.2.3.4:8484"],
                                            'paritionInSyncReplicas': ["4.2.3.5:8484", "1.2.3.4:8484"]}
                                    ]
                 },
              {"name": "yourTopic", 'acls': [{'id': '443', 'username': "aaa", 'permission_type': "Allow", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}], 'shares': []},
              {"name": "thisTopic", 'acls': [{'id': '123', 'username': "ccc", 'permission_type': "Deny", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}], 'shares': []},
              {"name": "emptyTopic", 'acls': [], 'shares': []}
            ];

            self.topicDetails = {"name": ""};
                    
            self.maxNumTopics = 10;
            self.numTopicsUsed = 0;

            self.currentTopic = "";
            self.topicName = "";
            self.numReplicas = "";
            self.numPartitions = "";
            self.username = "admin@kth.se";
            self.permission_type = "Allow";
            self.operation_type = "Read";
            self.host = "*";
            self.role = "*";
            self.activeId = -1;
            self.activeShare = -1;
            self.selectedProjectName="";

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
              self.activeId = acl.id;
            };

            self.updateAcl = function (topicName, aclId){
              var acl ={};
              acl.role = self.role;
              acl.username = self.username;
              acl.permissionType = self.permission_type;
              acl.operationType = self.operation_type;
              acl.host = self.host;
                KafkaService.updateTopicAcl(self.projectId, topicName, aclId, acl).then(
                        function(success){
                            self.getAclsForTopic(topicName);
                            
                        }, function(error){
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                            
                        });
            }

            self.getAllTopics = function () {
              KafkaService.getTopics(self.projectId).then(
                      function (success) {
                        self.topics = success.data;
//                        console.log('the list of topics');
//                        self.topics.forEach(function (item){
//                           console.log(item.name);
//                        });
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

            self.getTopicDetails = function (topicName) {
                KafkaService.getTopicDetails(self.projectId, topicName).then(
                        function (success) {
                            console.log(topicName);
                            for(var i =0;i<self.topics.length;i++){
                              if(self.topics[i].name === topicName){
                                  console.log('this is the topicName: '+self.topics[i].name)
                                  self.topics[i].partitionDetails= success.data;

                                  return;
                              }
                          }
                        }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
               });
            }
                
//            self.getAllTopics();


            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.createTopic = function () {
//              KafkaService.createTopic(self.projectId, self.topicDetails).then(
//                      function (success) {
//                        self.getAllTopics();
//                      }, function (error) {
//                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
//              });

// Check if available topics. If not, print a growl error, then return.

              ModalService.createTopic('lg', self.projectId).then(
                      function (success) {
                      }, function (error) {
                //The user changed their mind.
              });
              self.getAllTopics();

            };

            self.removeTopic = function (topicName) {
              ModalService.confirm("sm", "Delete Topic (" + topicName + ")",
                      "Do you really want to delete this topic? This action cannot be undone.")
                      .then(function (success) {
                        KafkaService.removeTopic(self.projectId, topicName).then(
                                function (success) {
                                  self.getAllTopics();
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
                          for(var i =0;i<self.topics.length;i++){
                              if(self.topics[i].name === topicName){
                                  self.topics[i].acls = success.data;
                                  return;
                              }
                          }
                        self.activeId = "";
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to get ACLs for the topic', ttl: 5000});
              });
            };

            self.addAcl = function (topicName) {
                
                ModalService.createTopicAcl('lg', self.projectId, topicName).then(
                      function (success) {
                          self.getAclsForTopic(topicName);
                      }, function (error) {
                //The user changed their mind.
                growl.error(error.data.errorMsg, {title: 'adding acl', ttl: 5000});

              });
                self.getAclsForTopic(topicName);
   
            };

            self.removeAcl = function (topicName, aclId) {
              KafkaService.removeTopicAcl(self.projectId, topicName, aclId).then(
                      function (success) {
                        self.getAclsForTopic(topicName);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to remove topic', ttl: 5000});
              });

            };
            
            
            self.shareTopic = function(topicName) {
              ModalService.selectProject('lg', "/[^]*/",
                      "Select a Project to share the topic with.").then(
                      function (success) {
                        var destProj = success.projectId;
                        if(destProj!==self.projectId){
                        KafkaService.shareTopic(self.projectId, topicName, destProj).then(
                                function (success) {
                                  self.topicIsSharedTo(topicName);
                                  growl.success(success.data.successMessage, {title: 'Topic shared successfully with project: ' + destProj.name, ttl: 2000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });

                        }else{
                            growl.info(success.name+" is owner of topic.", {title:success.getname+"Topic not shared", ttl:500});
                            
                        }
                      }, function (error) {
                //The user changed their mind.
              });

            };
              
            self.unshareTopic = function(topicName, projectId) {
//              ModalService.selectProject('lg', "/[^]*/",
//                      "problem selecting project").then(
//                      function (success) {
//                        var destProj = success;

                        KafkaService.unshareTopic(self.projectId, topicName, projectId).then(
                                function (success) {
                                  self.topicIsSharedTo(topicName);
                                  growl.success(success.data.successMessage, {title: 'Topic share removed (unshared) rom project: ' + destProj.name, ttl: 2000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });


//                      }, function (error) {
//                //The user changed their mind.
//              });

            };
            
            self.topicIsSharedTo = function (topicName) {
                KafkaService.topicIsSharedTo(this.projectId, topicName).then(
                        function (success) {                         
                           for(var i =0;i<self.topics.length;i++){
                              if(self.topics[i].name === topicName){
                                  self.topics[i].shares=success.data;
                                  return;
                              }
                          }
                        }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Failed to get topic sharing information', ttl: 5000});
                });
            };
              
          }]);



