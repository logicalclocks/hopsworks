angular.module('hopsWorksApp')
        .controller('CreateTopicCtrl', ['$uibModalInstance', 'KafkaService',
        'growl', 'projectId', 'TourService', 'projectIsGuide',
          function ($uibModalInstance, KafkaService, growl, projectId,
          TourService, projectIsGuide) {

            var self = this;
            self.projectId = projectId;
            self.tourService = TourService;
            self.projectIsGuide = projectIsGuide;
            self.selectedProjectName;
            self.topicName;
            self.num_partitions;
            self.num_replicas;
            self.max_num_replicas;
            self.topicName_wrong_value = 1;
            self.replication_wrong_value = 1;
            self.topicSchema_wrong_value = 1;
            self.wrong_values = 1;
            self.working = false;
            
            self.schemas = [];
            self.schema;
            self.schemaVersion;
            
            self.init = function() {
                if (self.projectIsGuide) {
                  self.tourService.resetTours();
                  self.tourService.currentStep_TourFive = 0;
                }

                KafkaService.defaultTopicValues(self.projectId).then(
                    function (success) {
                    self.num_partitions = success.data.numOfPartitions;
                    self.num_replicas = success.data.numOfReplicas;
                    self.max_num_replicas = success.data.maxNumOfReplicas;
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Could not get defualt topic values', ttl: 5000, referenceId: 21});
                   });
                   
               KafkaService.getSchemasForTopics(self.projectId).then(
                    function (success){
                      self.schemas = success.data;
                    }, function (error) {
                      growl.error(error.data.errorMsg, {title: 'Could not get schemas for topic', ttl: 5000, referenceId: 21});
                      });
            };
            
            self.init();            

            self.guidePopulateTopic = function () {
              self.topicName = self.tourService.kafkaTopicName
                + "_" + self.projectId;
              self.num_partitions = 2;
              self.num_replicas = 1;

              for (var i = 0; i < self.schemas.length; i++) {
                if (angular.equals(self.schemas[i].name, self.tourService
                  .kafkaSchemaName + "_" + self.projectId)) {
                  self.schema = self.schemas[i];
                  break;
                }
              }

              self.schemaVersion = 1;
            };

            self.createTopic = function () {
              self.working = true;
              self.wrong_values = 1;
              self.replication_wrong_value = 1;
              self.topicName_wrong_value = 1;
              self.topicSchema_wrong_value = 1;
              
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
              
              if(!self.schema || !self.schemaVersion){
                self.topicSchema_wrong_value = -1;
                self.wrong_values=-1;
              }
              else{
                self.topicSchema_wrong_value = 1;
              }
              
              if(self.wrong_values === -1){
                  self.working = false;
                  return;
              }
              
              var topicDetails ={};
              topicDetails.name=self.topicName;
              topicDetails.numOfPartitions =self.num_partitions;
              topicDetails.numOfReplicas =self.num_replicas;
              topicDetails.schemaName = self.schema.name;
              topicDetails.schemaVersion = self.schemaVersion;                                  

              KafkaService.createTopic(self.projectId, topicDetails).then(
                      function (success) {
                        self.working = false;
                          $uibModalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to create topic', ttl: 5000});
                        self.working = false;
              });      
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);