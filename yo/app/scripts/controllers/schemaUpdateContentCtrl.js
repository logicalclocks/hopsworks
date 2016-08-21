angular.module('hopsWorksApp')
        .controller('SchemaUpdateContentCtrl', ['$modalInstance', '$scope', 'KafkaService', 'growl', 'projectId', 'schemaName', 'schemaVersion',
            function ($modalInstance, $scope, KafkaService, growl, projectId, schemaName, schemaVersion) {

                var self = this;
                self.projectId = projectId;
                self.schemaName = schemaName;
                self.contents;
                self.schemaVersion = schemaVersion;
                self.content_empty = 1;
                self.message ="";
                self.validSchema = "invalid";

                self.init = function () {

                    KafkaService.getSchemaContent(self.projectId, schemaName, self.schemaVersion).then(
                            function (success) {
                                $scope.jsonObj = success.data.contents;
                            }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Could not get schema for topic', ttl: 5000, referenceId: 21});
                    });
                };

                self.init();
                
                self.validateSchema = function () {
                   
                    self.validSchema = "invalid";
                    
                    var ugly = document.getElementById('myPrettyJson');
                    self.contents = ugly.textContent;
                    
                    if(!self.contents){
                        self.content_empty = -1;
                        self.wrong_values = -1;
                    }

                    if(self.wrong_values === -1){
                        return;
                    }

                    var schemaDetail ={};
                    schemaDetail.name=self.schemaName;
                    schemaDetail.contents =self.contents;
                    //schemaDetail.version =self.version;
                    schemaDetail.versions =[];

                    KafkaService.validateSchema(self.projectId, schemaDetail).then(
                            function (success) {
                                self.message = "schema is valid";
                                self.validSchema="";
                            }, function (error) {
                                self.message = error.data.errorMsg;
                    });
                 };
                
                self.createSchema = function () {
                    
                    var ugly = document.getElementById('myPrettyJson');
                    self.contents = ugly.textContent;
                    
                    var schemaDetail = {};
                    schemaDetail.name = schemaName;
                    schemaDetail.contents =self.contents;
                    schemaDetail.version = self.schemaVersion + 1;

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