/**
 * Created by ermiasg on 2015-05-17.
 */
'use strict';


angular.module('hopsWorksApp')
    .controller('DataSetCreatorCtrl', ['$modalInstance','DataSetService', '$routeParams','growl', 'dataSet',
        function ( $modalInstance, DataSetService, $routeParams, growl, dataSet) {

            var self = this;

            self.datasets = [];
            self.temps = [{'temp':"temp"}];
            self.dataSet = {'name': "", 'description': "", 'template': "", 'searchable': true}
            var pId = $routeParams.projectID;
            var dataSetService = DataSetService(pId);

            var createDataSetDir = function (dataSet) {
                dataSetService.createDataSetDir(dataSet).then(
                    function (success) {
                        $modalInstance.close(success);
                    }, function (error) {
                        console.log("createDataSetDir error");
                        console.log(error);
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                    });
            };

            self.close = function () {
                $modalInstance.dismiss('cancel');
            };

            self.saveDataSetDir = function () {
                if (dataSet) {
                    self.dataSet.name = dataSet + '/' + self.dataSet.name;
                    createDataSetDir(self.dataSet);
                }else{
                    createDataSetDir(self.dataSet);
                }

            };

        }]);

