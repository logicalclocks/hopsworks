angular.module('hopsWorksApp')
        .controller('SchemaCreateCtrl', ['$uibModalInstance', 'KafkaService',
         'TourService', 'growl', 'projectId', 'projectIsGuide',
          function ($uibModalInstance, KafkaService, TourService, growl,
          projectId, projectIsGuide) {

            var self = this;
            self.tourService = TourService;
            self.projectId = projectId;
            self.schemaName;
            self.content;
            self.version;
            self.message ="";
            self.validSchema = "valid";
            self.projectIsGuide = projectIsGuide;

            self.init = function(){
              if (self.projectIsGuide) {
                self.tourService.resetTours();
                self.tourService.currentStep_TourFour = 0;
              }
            };

            self.init();

            self.guidePopulateSchema = function () {
              self.schemaName = self.tourService.kafkaSchemaName
                + "_" + self.projectId;
              var demoSchema = new Object();
              demoSchema.fields = [{"name":"timestamp","type":"string"},
                {"name":"priority","type":"string"},
                {"name":"logger","type":"string"},
                {"name":"message","type":"string"}];
              demoSchema.name = "myrecord";
              demoSchema.type = "record";

              var jsonStr = JSON.stringify(demoSchema);
              self.content = jsonStr;
              self.version = 1;
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
                          if (self.projectIsGuide) {
                            self.tourService.resetTours();
                            self.tourService.currentStep_TourFour = 1;
                          }
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
                          $uibModalInstance.close(success);
                          if (self.projectIsGuide) {
                            self.tourService.resetTours();
                          }
                      }, function (error) {
                          self.message = error.data.errorMsg;
                          self.validSchema="invalid";
              });      
            };
            
            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

