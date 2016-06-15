angular.module('hopsWorksApp')
        .controller('SchemaUpdateContentCtrl', ['$modalInstance', '$scope', 'KafkaService', 'growl', 'projectId', 'schemaName', 'schemaVersion',
            function ($modalInstance, $scope, KafkaService, growl, projectId, schemaName, schemaVersion) {

                var self = this;
                self.projectId = projectId;
                self.schemaName = schemaName;
                self.contents;
                self.schemaVersion = schemaVersion;
                self.content_empty = 1;

                self.init = function () {

                    KafkaService.getSchemaContent(self.projectId, schemaName, self.schemaVersion+1).then(
                            function (success) {
                                $scope.jsonObj = success.data.contents;
                            }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Could not get schema for topic', ttl: 5000, referenceId: 21});
                    });
                };

                self.init();

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
                        growl.error(error.data.errorMsg, {title: 'Failed to update the schema', ttl: 5000});
                    });
                };

                self.close = function () {
                    $modalInstance.dismiss('cancel');
                };
            }]);