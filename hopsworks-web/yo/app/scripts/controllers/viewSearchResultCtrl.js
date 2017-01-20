angular.module('hopsWorksApp')
        .controller('ViewSearchResultCtrl', ['$scope', '$uibModalInstance', 'RequestService', 'DataSetService',  'growl', 'response', 'result', 'projects','$showdown',
          function ($scope, $uibModalInstance, RequestService, DataSetService, growl, response, result, projects, $showdown) {
            var self = this;
            self.request = {'inodeId': "", 'projectId': "", 'message': ""};
            self.projects = projects;
            self.content = result;
            if (result.type === 'proj') {
              self.type = 'Project';
              self.requestType = 'join';
              self.infoMembers = 'Members in this project.';
              self.infoDS = 'Datasets in this project.';
              self.result = response;
              self.request.projectId = self.result.projectId;

              self.sendRequest = function () {
                RequestService.joinRequest(self.request).then(
                        function (success) {
                          $uibModalInstance.close(success);
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000, referenceId: 21});
                });
              };
            } else if (result.type === 'inode' || result.type === 'ds') {
              self.type = 'Dataset';
              self.requestType = 'access';
              self.infoMembers = 'Members of the owning project.';
              self.infoDS = 'Projects this dataset is shared with.';
              self.result = response;
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
            $scope.readme = null;
            
            self.getReadme = function () {
              if ($scope.readme !== null) {
                return;
              }
              var filePath = self.content.details.path;
              if (filePath === undefined || filePath === '') {
                $scope.readme = null;
                return;
              }
              filePath = filePath + '/README.md';
              dataSetService.getReadme(filePath).then(
                        function (success) {
                          var content = success.data.content;
                          $scope.readme = $showdown.makeHtml(content);
                        }, function (error) {
                  //To hide README from UI
                  growl.error(error.data.errorMsg, {title: 'Error retrieving README file', ttl: 5000, referenceId: 3});
                  $scope.readme = null;
                });
            };

            self.sizeOnDisk = function (fileSizeInBytes) {
              return convertSize(fileSizeInBytes);
            };
            
            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

