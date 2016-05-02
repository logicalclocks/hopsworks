angular.module('hopsWorksApp')
        .controller('CreateTopicCtrl', ['$modalInstance', 'ProjectService', 'KafkaService', 'growl', 'projectId', 
          function ($modalInstance, ProjectService, KafkaService, growl, projectId) {

            var self = this;
            self.projectId = projectId;
            self.selectedProjectName;
            self.topicName;
//            self.partitionId = 1;
//            self.partitionLeader;
            self.partitionReplicas;
            self.partitionInsyncReplicas;
            
            self.init = function() {
                          KafkaService.defaultTopicValues().then(
                                function (success) {
//                                  self.partitionId = success.data.partitionId;
//                                  self.partitionLeader = success.data.partitionLeader;
                                  self.partitionReplicas = success.data.partitionReplicas;
                                  self.partitionInsyncReplicas = success.data.partitionInsyncReplicas;
                                  
                                }, function (error) {
                                growl.error(error.data.errorMsg, {title: 'Could not get list of Projects', ttl: 5000, referenceId: 21});
                        });
            };
            
            self.init();            

            
            self.createTopic = function () {
              KafkaService.createTopic(self.projectId, self.topicName,  self.partitionId, self.partitionLeader).then(
                      function (success) {
                        self.getAllTopics();
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
              });      
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

