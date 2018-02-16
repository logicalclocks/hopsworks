/*
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

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

              var jsonStr = JSON.stringify(demoSchema, null, '\t');
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

