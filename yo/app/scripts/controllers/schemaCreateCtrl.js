angular.module('hopsWorksApp')
        .controller('SchemaCreateCtrl', ['$modalInstance', 'KafkaService', 'growl', 'projectId', 
          function ($modalInstance, KafkaService, growl, projectId) {

            var self = this;
            self.projectId = projectId;
            self.schemaName;
            self.content;
            self.version;
            self.schemaName_empty = 1;
            self.content_empty = 1;
            self.wrong_values=1;
            
            self.createSchema = function () {
              
              if(!self.schemaName){
                  self.schemaName_empty = -1;
                  self.wrong_values = -1;
              }
              
              if(!self.content){
                  self.content_empty = -1;
                  self.wrong_values = -1;
              }
              
              if(self.wrong_values === -1){
                  return;
              }
              
            var schemaDetail ={};
              schemaDetail.name=self.schemaName;
              schemaDetail.contents =self.content;
              schemaDetail.version =self.version;
              schemaDetail.versions =[];

              KafkaService.createSchema(self.projectId, schemaDetail).then(
                      function (success) {
                          $modalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to create the schema', ttl: 5000});
              });      
            };
            
            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

