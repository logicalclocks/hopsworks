angular.module('hopsWorksApp')
        .controller('CreateTopicAclCtrl', ['$modalInstance', 'KafkaService', 'growl', 'projectId', 
          function ($modalInstance, KafkaService, growl, projectId) {

            var self = this;
            self.projectId = projectId;
            self.selectedProjectName;
            self.topicName;
            self.aclName;
            self.permission_type;
            
            self.init = function() {
                          KafkaService.defaultTopicValues().then(
                                function (success) {
                                  
                                }, function (error) {
                                growl.error(error.data.errorMsg, {title: 'Could not get list of Projects', ttl: 5000, referenceId: 21});
                        });
            };
            
            self.init();            

            
            self.createTopicAcl = function () {
              KafkaService.createTopicAcl(self.projectId, self.topicName,  self.partitionId, self.partitionLeader).then(
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

