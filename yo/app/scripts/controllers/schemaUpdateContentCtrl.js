angular.module('hopsWorksApp')
        .controller('SchemaUpdateContentCtrl', ['$modalInstance', 'KafkaService', 'growl', 'projectId', 'schemaName', 'schemaVersion',
          function ($modalInstance, KafkaService, growl, projectId, schemaName, schemaVersion) {

            var self = this;
            self.projectId = projectId;
            self.schemaName = schemaName;
            self.content;
            self.schemaVersion= schemaVersion;
            self.content_empty = 1;
            
            self.createSchema = function () {
              
              if(!self.content){
                  self.content_empty = -1;
              }
              
            var schemaDetail ={};
              schemaDetail.name=schemaName;
              schemaDetail.contents =self.content;
              schemaDetail.version =self.schemaVersion;

              KafkaService.createSchema(self.projectId, schemaDetail).then(
                      function (success) {
                          $modalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to update the schema', ttl: 5000});
              });      
            };
            
            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

