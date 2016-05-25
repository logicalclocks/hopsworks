angular.module('hopsWorksApp')
        .controller('CreateAclCtrl', ['$modalInstance', 'KafkaService', 'growl', 'projectId', 'topicName',
          function ($modalInstance, KafkaService, growl, projectId, topicName) {

            var self = this;
            self.users =[];
            self.projectId = projectId;
            self.topicName = topicName;
            self.permission_type;
            
            self.username = "admin@kth.se";
            self.permission_type = "Allow";
            self.operation_type = "Read";
            self.host = "*";
            self.role = "*";
            self.selectedProjectName="";
            
            self.init = function() {
                KafkaService.aclUsers(self.projectId, self.topicName).then(
                    function (success) {
                        self.users = success.data;
                }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Could not load ACL users', ttl: 5000, referenceId: 21});
                   });
            };
            
            self.init();
            
            self.createTopicAcl = function () {
                
                var acl = {};
//              acl.topic_name = self.topicName;
//              acl.project_id = self.projectId;
              acl.role = self.role;
              acl.username = self.username.userName;
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

