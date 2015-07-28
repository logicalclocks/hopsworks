angular.module('hopsWorksApp')
    .controller('ViewSearchResultCtrl', ['$modalInstance', 'result','projectOrDataset',
        function ($modalInstance, result, projectOrDataset) {

            var self = this;

            if (projectOrDataset === 'parent') {
                self.type = 'Project';
                self.requestType = 'join';
                self.infoMembers = 'Members in this project.';
                self.infoDS = 'Datasets in this project.';               
                self.result = result;
            } else if (projectOrDataset === 'child') {
                self.type = 'Dataset';
                self.requestType = 'access';
                self.infoMembers = 'Members of the owning project.';
                self.infoDS = 'Projects this dataset is shared with.';
                self.result = result;
            }

            self.sendRequest = function () {

            };

            self.close = function () {
                $modalInstance.dismiss('cancel');
            };
        }]);

