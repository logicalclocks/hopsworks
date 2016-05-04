angular.module('hopsWorksApp')
        .controller('CreateAclCtrl', ['$modalInstance', 'KafkaService', 'growl', 'projectId', 'topicName',
          function ($modalInstance, KafkaService, growl, projectId, topicName) {

            var self = this;
            self.projectId = projectId;
            self.topicName = topicName;
            self.permission_type;
            
            self.username = "admin@kth.se";
            self.permission_type = "Allow";
            self.operation_type = "Read";
            self.host = "*";
            self.role = "*";
            self.selectedProjectName="";
          
            
            self.createTopicAcl = function () {
                
                var acl = {};
//              acl.topic_name = self.topicName;
//              acl.project_id = self.projectId;
              acl.role = self.role;
              acl.username = self.username;
              acl.permissionType = self.permission_type;
              acl.operationType = self.operation_type;
              acl.host = self.host;

              KafkaService.createTopicAcl(self.projectId, topicName, acl).then(
                      function (success) {
                          $modalInstance.close(success);
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Failed to add topic', ttl: 5000});
              });
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

