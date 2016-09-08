angular.module('hopsWorksApp')
        .controller('ViewSearchResultCtrl', ['$uibModalInstance', 'RequestService', 'DataSetService',  'growl', 'result', 'datatype', 'projects',
          function ($uibModalInstance, RequestService, DataSetService, growl, result, datatype, projects) {

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
                          $uibModalInstance.close(success);
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
                });
              };
            } else if (datatype === 'inode' || datatype === 'ds') {

              self.type = 'Dataset';
              self.requestType = 'access';
              self.infoMembers = 'Members of the owning project.';
              self.infoDS = 'Projects this dataset is shared with.';
              self.result = result;
              self.request.inodeId = self.result.inodeId;
              self.projectId = self.result.projectId;              

              self.sendRequest = function () {
                RequestService.accessRequest(self.request).then(
                        function (success) {
                          $uibModalInstance.close(success);
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
                });
              };
            }

            var dataSetService = DataSetService(self.projectId); 

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

