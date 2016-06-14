angular.module('hopsWorksApp')
        .controller('SchemaUpdateContentCtrl', ['$modalInstance', '$scope', 'KafkaService', 'growl', 'projectId', 'schemaName', 'schemaVersion',
            function ($modalInstance, $scope, KafkaService, growl, projectId, schemaName, schemaVersion) {

                var self = this;
                self.projectId = projectId;
                self.schemaName = schemaName;
                self.content;
                self.schemaVersion = schemaVersion;
                self.content_empty = 1;



                self.init = function () {

                    KafkaService.getSchemaContent(self.projectId, schemaName, self.schemaVersion).then(
                            function (success) {
//                                self.schemaContents = success.data;
                                $scope.jsonObj = success.data.contents;
//                                self.schemaContents = JSON.stringify($scope.jsonObj);
                            }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Could not get schema for topic', ttl: 5000, referenceId: 21});
                    });
                };

                self.init();

//
//                $scope.getObjectAsText = function () {
//                    $scope.schemaContents = JSON.stringify($scope.containerObject);
//                };
//

                self.createSchema = function () {

                    if (!self.content) {
                        self.content_empty = -1;
                    }

                    var schemaDetail = {};
                    schemaDetail.name = schemaName;
                    schemaDetail.contents =self.schemaContents;
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

