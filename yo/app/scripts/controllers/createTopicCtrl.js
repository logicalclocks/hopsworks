angular.module('hopsWorksApp')
        .controller('CreateTopicCtrl', ['$modalInstance', 'KafkaService', 'growl', 'projectId', 
          function ($modalInstance, KafkaService, growl, projectId) {

            var self = this;
            self.projectId = projectId;
            self.selectedProjectName;
            self.topicName;
            self.num_partitions;
            self.num_replicas;
            self.max_num_replicas;
            self.topicName_wrong_value = 1;
            self.replication_wrong_value = 1;
            self.wrong_values = 1;
            
            self.init = function() {
                KafkaService.defaultTopicValues(self.projectId).then(
                    function (success) {
                    self.num_partitions = success.data.numOfPartitions;
                    self.num_replicas = success.data.numOfReplicas;
                    self.max_num_replicas = success.data.maxNumOfReplicas;
                                  
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Could not get defualt topic values', ttl: 5000, referenceId: 21});
                   });
            };
            
            self.init();            
            
            
            self.createTopic = function () {
              
              if(self.max_num_replicas < self.num_replicas){
                self.replication_wrong_value =-1;
                self.wrong_values=-1;
              }else{
                self.replication_wrong_value =1;
              }
              //check topic name
              if(!self.topicName){
                self.topicName_wrong_value = -1;
                self.wrong_values=-1;
              }
              else{
                self.topicName_wrong_value = 1;
              }
              
              if(self.wrong_values ===-1){
                  return;
              }
              
              var topicDetails ={};
              topicDetails.name=self.topicName;
              topicDetails.numOfPartitions =self.num_partitions;
              topicDetails.numOfReplicas =self.num_replicas;

              KafkaService.createTopic(self.projectId, topicDetails).then(
                      function (success) {
                          $modalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
              });      
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

