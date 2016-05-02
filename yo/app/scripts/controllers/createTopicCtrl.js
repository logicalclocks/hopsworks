angular.module('hopsWorksApp')
        .controller('CreateTopicCtrl', ['$modalInstance', 'KafkaService', 'growl', 'projectId', 
          function ($modalInstance, KafkaService, growl, projectId) {

            var self = this;
            self.projectId = projectId;
            self.selectedProjectName;
            self.topicName;
            self.num_paritions;
            self.partitioning_factor;
            
            self.init = function() {
                          KafkaService.defaultTopicValues(self.projectId).then(
                                function (success) {
                                  self.num_paritions = success.data.numOfPartitions;
                                  self.partitioning_factor = success.data.numOfReplicas;
                                  
                                }, function (error) {
                                growl.error(error.data.errorMsg, {title: 'Could not get defualt topic values', ttl: 5000, referenceId: 21});
                        });
            };
            
            self.init();            

            
            self.createTopic = function () {
                var topicDetails ={};
                topicDetails.name=self.topicName;
                topicDetails.numOfPartitions =self.num_paritions;
                topicDetails.numOfReplicas =self.partitioning_factor;

              KafkaService.createTopic(self.projectId, topicDetails).then(
                      function (success) {
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
              });      
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

