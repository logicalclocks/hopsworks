angular.module('hopsWorksApp')
        .controller('SchemaCreateCtrl', ['$uibModalInstance', 'KafkaService',
         'TourService', 'growl', 'projectId', 'isGuide',
          function ($uibModalInstance, KafkaService, TourService, growl,
          projectId, isGuide) {

            var self = this;
            self.tourService = TourService;
            self.projectId = projectId;
            self.schemaName;
            self.content;
            self.version;
            self.message ="";
            self.validSchema = "valid";
            self.isGuide = isGuide;

            self.init = function(){
              if (self.isGuide === true) {
                self.tourService.currentStep_TourFour = 0;
              }
            };

            self.init();

            self.guidePopulateSchema = function () {
              self.schemaName = "DemoAvroName";
              var demoSchema = new Object();
              demoSchema.fields = [{"name": "platform", "type": "string"},
                  {"name": "program", "type": "string"}];
              demoSchema.name = "myrecord";
              demoSchema.type = "record";

              jsonStr = JSON.stringify(demoSchema);
              self.content = jsonStr;
            };

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

            // NOTICE: Use only for the guided tours
            self.guideValidateSchema = function () {
              self.tourService.currentStep_TourFour = 1;
              self.validateSchema();
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
                          $uibModalInstance.close(success);
                      }, function (error) {
                          self.message = error.data.errorMsg;
                          self.validSchema="invalid";
              });      
            };
            
            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

