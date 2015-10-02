angular.module('hopsWorksApp')
        .controller('ViewSearchResultCtrl', ['$modalInstance', 'RequestService', 'growl', 'result', 'datatype', 'projects',
          function ($modalInstance, RequestService, growl, result, datatype, projects) {

            var self = this;
            self.request = {'inodeId': "", 'projectId': "", 'message': ""};
            self.projects = projects;

            if (datatype === 'project') {
              self.type = 'Project';
              self.requestType = 'join';
              self.infoMembers = 'Members in this project.';
              self.infoDS = 'Datasets in this project.';
              self.result = result;
              self.request.projectId = self.result.projectId;

              self.sendRequest = function () {
                RequestService.joinRequest(self.request).then(
                        function (success) {
                          $modalInstance.close(success);
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
                });
              };
            } else if (datatype === 'child' || datatype === 'dataset') {

              self.type = 'Dataset';
              self.requestType = 'access';
              self.infoMembers = 'Members of the owning project.';
              self.infoDS = 'Projects this dataset is shared with.';
              self.result = result;
              self.request.inodeId = self.result.inodeId;

              self.sendRequest = function () {
                RequestService.accessRequest(self.request).then(
                        function (success) {
                          $modalInstance.close(success);
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
                });
              };
            }

            self.close = function () {
              $modalInstance.dismiss('cancel');
            };
          }]);

