angular.module('hopsWorksApp')
        .controller('SelectProjectCtrl', ['$modalInstance', 'ProjectService', 'growl', 'projectId', 'msg', 
          function ($modalInstance, ProjectService, growl, projectId, msg) {

            var self = this;
            self.projectId = projectId;
            self.msg = msg;
            self.selectedProjectName;
            self.projects = [];

            self.init = function() {
                        ProjectService.query().$promise.then(
                                function (success) {
                                  self.projects = success;
                                }, function (error) {
                                growl.error(error.data.errorMsg, {title: 'Could not get list of Projects', ttl: 5000, referenceId: 21});
                        });
            };
            
            self.init();

            self.selectProject = function () {
              if (self.selectedProjectName === undefined || self.selectedProjectName === "") {
                growl.error("Could not select a project", {title: 'Error', ttl: 5000, referenceId: 21});
                return;
              }

                        ProjectService.getProjectInfo({projectName: self.selectedProjectName}).$promise.then(
                                function (success) {
                                  $modalInstance.close(success);
                                }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 10000});
                        });              
            };

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

