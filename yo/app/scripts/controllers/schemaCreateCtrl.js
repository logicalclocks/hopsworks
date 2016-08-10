angular.module('hopsWorksApp')
        .controller('SchemaCreateCtrl', ['$modalInstance', 'KafkaService', 'growl', 'projectId', 
          function ($modalInstance, KafkaService, growl, projectId) {

            var self = this;
            self.projectId = projectId;
            self.schemaName;
            self.content;
            self.version;
            self.message ="";
            self.validSchema = "valid";
            
            self.validateSchema = function () {
                self.validSchema = "valid";
                
               self.schemaName_empty = 1;
               self.content_empty = 1;
               self.wrong_values=1;
                
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
                //schemaDetail.version =self.version;
                schemaDetail.versions =[];
                  
              KafkaService.validateSchema(self.projectId, schemaDetail).then(
                      function (success) {
                          self.message = "schema is valid";
                          self.validSchema="";
                      }, function (error) {
                          self.message = error.data.errorMsg;;//   "schema is invalid";
              });
            };
            
            self.createSchema = function () {
                
               self.schemaName_empty = 1;
               self.content_empty = 1;
               self.wrong_values=1;
              
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
              //schemaDetail.version =self.version;
              schemaDetail.versions =[];

              KafkaService.createSchema(self.projectId, schemaDetail).then(
                      function (success) {
                          $modalInstance.close(success);
                      }, function (error) {
                          self.message = error.data.errorMsg;
                          self.validSchema="invalid";
              });      
            };
            
            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

