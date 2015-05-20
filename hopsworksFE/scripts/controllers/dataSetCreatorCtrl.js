/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

/**
 * Created by ermiasg on 2015-05-17.
 */
'use strict';


angular.module('hopsWorksApp')
    .controller('DataSetCreatorCtrl', ['$modalInstance','DataSetService', 'MetadataActionService', '$routeParams','growl', 'dataSet',
        function ($modalInstance, DataSetService, MetadataActionService, $routeParams, growl, dataSet) {

            var self = this;

            self.datasets = [];
            self.selectedTemplate = {};
            self.temps = [{'temp':"temp"}];
            self.dataSet = {'name': "", 'description': "", 'template': "", 'searchable': true};
            var pId = $routeParams.projectID;
            var dataSetService = DataSetService(pId);

            self.templates = [];
            
            MetadataActionService.fetchTemplates()
                .then(function(response){
                    var temps = JSON.parse(response.board);
                    angular.forEach(temps.templates, function(value, key){
                        self.templates.push(value);
                    });
                });
           
            var createDataSetDir = function (dataSet) {
                dataSetService.createDataSetDir(dataSet)
                    .then(function (success) {
                        $modalInstance.close(success);
                    }, 
                    function (error) {
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
                    console.log("SELECTED THE TEMPLATE " + JSON.stringify(self.selectedTemplate));
                    self.dataSet.name = dataSet + '/' + self.dataSet.name;
                    self.dataSet.template = self.selectedTemplate.id;
                    
                    console.log("SSDFSDFSD FSD F SD SD " + JSON.stringify(self.dataSet));
                    
                    createDataSetDir(self.dataSet);
                }else{
                    createDataSetDir(self.dataSet);
                }
            };
        }]);

