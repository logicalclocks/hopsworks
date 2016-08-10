angular.module('hopsWorksApp')
        .controller('SelectProjectCtrl', ['$modalInstance', 'ProjectService', 'growl', 'global', 'projectId', 'msg',
          function ($modalInstance, ProjectService, growl, global, projectId, msg) {

            var self = this;
            self.global = global;
            self.projectId = parseInt(projectId);
            self.msg = msg;
            self.selectedProject;
            self.projects = [];

            self.init = function () {
              if (global) {
                ProjectService.getAll().$promise.then(
                        function (success) {
                          var j=0;
                          for(var i=0;i<success.length;i++){
                              if(success[i].id !== self.projectId){
                                  self.projects[j++] =success[i];
                              }
                          }
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Could not get list of Projects', ttl: 5000, referenceId: 21});
                });
              } else {
                ProjectService.query().$promise.then(
                        function (success) {
                          var j=0;
                          for(var i=0;i<success.length;i++){
                              if(success[i].id !== self.projectId){
                                  self.projects[j++] =success[i];
                              }
                          }
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Could not get list of Projects', ttl: 5000, referenceId: 21});
                });
              }
            };

            self.init();

            self.selectProject = function () {
              if (self.selectedProject === undefined || self.selectedProject === "") {
                growl.error("Could not select a project", {title: 'Error', ttl: 5000, referenceId: 21});
                return;
              }

              ProjectService.getProjectInfo({projectName: self.selectedProject.name}).$promise.then(
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

