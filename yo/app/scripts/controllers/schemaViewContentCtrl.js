angular.module('hopsWorksApp')
        .controller('SchemaViewContentCtrl', ['$modalInstance', '$scope', 'KafkaService', 'growl', 'projectId', 'schemaName', 'schemaVersion',
          function ($modalInstance, $scope, KafkaService, growl, projectId, schemaName, schemaVersion) {

            var self = this;
            
            self.projectId = projectId;
            self.schemaVersion = schemaVersion;
            self.schemaContents =[];
            self.schemaContent;
            $scope.jsonObj = "";
            
            
            self.init = function() {
                   
              KafkaService.getSchemaContent(self.projectId, schemaName, self.schemaVersion).then(
                 function (success) {
                 self.schemaContents = success.data;
                 $scope.jsonObj = success.data.contents;
                 }, function (error) {
                 growl.error(error.data.errorMsg, {title: 'Could not get schema for topic', ttl: 5000, referenceId: 21});
                 });
              
             
            };
            
            self.init();            

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

