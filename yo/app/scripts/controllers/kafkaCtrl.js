/**
 * Controller for the kafka page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('KafkaCtrl', ['$scope', '$routeParams', 'growl', 'KafkaService', '$location', 'ModalService', '$interval', '$mdSidenav',
          function ($scope, $routeParams, growl, KafkaService, $location, ModalService, $interval, $mdSidenav) {
              

            var self = this;
            self.projectId = $routeParams.projectID;
            self.topics = [];
//                    [{"name": "myTopic", 
//                'acls': [{'id': '4563', 'userEmail': "bbb", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}, {'id': '999', 'userEmail': "hdhd", 'permission_type': "write", 'operation_type': "write", 'host': "*", 'role': "*", 'shared': "*"}],
//                'shares': [{'proj_name': 'shareOne'},{'proj_name': 'shareTwo'}],
//                'partitionDetails': [{ 'id' : "21212",
//                                            'leader': "1.2.3.4:8484",
//                                            'replicas' : ["4.2.3.5:8484", "2.2.3.5:5555", "1.2.3.4:8484"],
//                                            'inSyncReplicas': ["4.2.3.5:8484", "1.2.3.4:8484"]}
//                                    ]
//                 },
//              {"name": "emptyTopic", 'acls': [], 'shares': [], 'partitionDetails': []}
//            ];
            
            self.sharedTopics = [];
//                    [{"name": "", 'acls': [], 'shares': [], 'partitionDetails': []}
//            ];
            
            
            self.topicDetails = {};
//            {"name": ""};
                    
            self.maxNumTopics = 10;
            self.numTopicsUsed = 0;

            self.currentTopic = "";
            self.topicName = "";
            self.numReplicas = "";
            self.numPartitions = "";
//            self.userEmail = "admin@kth.se";
            self.projectName = "";
            self.userEmail = "";
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
              self.userEmail = acl.userEmail;
              self.permission_type = acl.permission_type;
              self.operation_type = acl.operation_type;
              self.host = acl.host;
              self.role = acl.role;
              self.activeId = acl.id;
            };

            self.updateAcl = function (topicName, aclId){
              var acl ={};
              acl.projectName = self.projectName;
              acl.role = self.role;
              acl.userEmail = self.userEmail;
              acl.permissionType = self.permission_type;
              acl.operationType = self.operation_type;
              acl.host = self.host;
                KafkaService.updateTopicAcl(self.projectId, topicName, aclId, acl).then(
                        function(success){
                            self.getAclsForTopic(topicName);
                            
                        }, function(error){
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                            
                        });
            };

            self.getAllTopics = function () {
              KafkaService.getTopics(self.projectId).then(
                      function (success) {
                        self.topics = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };


            self.getAllSharedTopics = function () {
              KafkaService.getSharedTopics(self.projectId).then(
                      function (success) {
                        self.sharedTopics = success.data;
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
              });
            };

            self.getTopicDetails = function (topicName) {
                KafkaService.getTopicDetails(self.projectId, topicName).then(
                        function (success) {
                            for(var i =0;i<self.topics.length;i++){
                              if(self.topics[i].name === topicName){
                                  self.topics[i].partitionDetails= success.data;
                                  return;
                              }
                          }
                        }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error  dfjdsfjldfj', ttl: 5000});
               });
            };
                
            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.createSchema = function () {

              ModalService.createSchema('lg', self.projectId).then(
                      function (success) {
                          growl.success(success.data.successMessage, {title: 'New schema added successfully project.', ttl: 2000});
                          self.getAllTopics();

                      }, function (error) {
                //The user changed their mind.
              });

            };
            /**
             * Navigate to the new job page.
             * @returns {undefined}
             */
            self.createTopic = function () {

              if(self.topics.length >10){
                  growl.info("Topic Creation aborted", {title: 'Topic limit reached', ttl: 2000});
                  return;
              }
              ModalService.createTopic('lg', self.projectId).then(
                      function (success) {
                          growl.success(success.data.successMessage, {title: 'New topic created successfully project.', ttl: 2000});
                          self.getAllTopics();

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
                          growl.success(success.data.successMessage, {title: 'New acl added for the topic: '+topicName, ttl: 2000});
                          self.getAclsForTopic(topicName);
                      }, function (error) {
                //The user changed their mind.
                growl.error(error.data.errorMsg, {title: 'adding acl', ttl: 5000});

              });
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
            
            //operation done from topic
            self.unshareTopic = function(topicName, project) {

                        KafkaService.unshareTopic(self.projectId, topicName, project.id).then(
                                function (success) {
                                  self.topicIsSharedTo(topicName);
                                  growl.success(success.data.successMessage, {title: 'Topic share removed (unshared) from project: ' + project.name, ttl: 2000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });


            };
            
            //operation done from project
            self.unshareTopicFromProject =function (topicName){
                KafkaService.unshareTopicFromProject(self.projectId, topicName).then(
                        function (success) {
                                  self.getAllSharedTopics();
                                  growl.success(success.data.successMessage, {title: 'Topic share removed (unshared) from project:.', ttl: 2000});
                                }, function (error) {
                          growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                        });
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

             self.init = function(){
                self.getAllTopics();
                self.getAllSharedTopics();
             };
            
            self.init();              
              
          }]);



