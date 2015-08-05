angular.module('hopsWorksApp')
    .controller('ViewSearchResultCtrl', ['$modalInstance','ProjectService','RequestService','growl', 'result','projectOrDataset',
        function ($modalInstance, ProjectService, RequestService, growl, result, projectOrDataset) {

            var self = this;
            self.request = {'inodeId':"", 'projectId':"",'message':""};
            self.projects = "";
            if (projectOrDataset === 'parent') {
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
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
                };
            } else if (projectOrDataset === 'child') {
                ProjectService.query().$promise.then(
                    function (success) {
                        self.projects = success;
                    }, function (error) {
                        console.log('Error: ' + error);
                    }
                );
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
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                        });
                };
            }



            self.close = function () {
                $modalInstance.dismiss('cancel');
            };
        }]);

