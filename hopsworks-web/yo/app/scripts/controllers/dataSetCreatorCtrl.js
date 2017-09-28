/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

/**
 * Created by ermiasg on 2015-05-17.
 */
'use strict';


angular.module('hopsWorksApp')
        .controller('DataSetCreatorCtrl', ['$cookies', '$uibModalInstance', 'DataSetService', 'MetadataActionService', '$routeParams', 'growl', 'path',
          function ($cookies, $uibModalInstance, DataSetService, MetadataActionService, $routeParams, growl, path) {

            var self = this;
            self.path = path;
            self.working = false;
            self.datasets = [];
            self.selectedTemplate = {};
            self.temps = [{'temp': "temp"}];
            self.dataSet = {'name': "", 'description': "", 'template': "", 'searchable': true, 'generateReadme': true};
            var pId = $routeParams.projectID;
            var dataSetService = DataSetService(pId);

            self.templates = [];

            self.regex = /^(?!.*?__|.*?&|.*? |.*?\/|.*\\|.*?\?|.*?\*|.*?:|.*?\||.*?'|.*?\"|.*?<|.*?>|.*?%|.*?\(|.*?\)|.*?\;|.*?#).*$/;

            MetadataActionService.fetchTemplates($cookies.get('email'))
                    .then(function (response) {
                      if (response.board != undefined && response.board !== null && response.status !== "ERROR") {
                        var temps = JSON.parse(response.board);
                        angular.forEach(temps.templates, function (value, key) {
                          self.templates.push(value);
                        });
                      }
                    }, function (error) {
                      console.log("ERROR " + JSON.stringify(error));
                    });

            var createDataSetDir = function (dataSet) {
              self.working = true;
              dataSetService.createDataSetDir(dataSet)
                      .then(function (success) {
                        self.working = false;
                        $uibModalInstance.close(success);
                      },
                              function (error) {
                                self.working = false;
                                growl.error(error.data.errorMsg, {title: 'Error', ttl: 10000});
                              });
            };

            var createTopLevelDataSet = function (dataSet) {
              self.working = true;
              dataSetService.createTopLevelDataSet(dataSet)
                      .then(function (success) {
                        self.working = false;
                        $uibModalInstance.close(success);
                      },
                              function (error) {
                                self.working = false;
                                growl.error(error.data.errorMsg, {title: 'Error', ttl: 10000});
                              });
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };

            self.saveDataSetDir = function () {
              self.dataSet.template = self.selectedTemplate.id;
              if (path) {
                //Assign it to new var to avoid showing the 
                var newDS = self.dataSet;
                newDS.name = path + '/' + newDS.name;
                createDataSetDir(newDS);
              } else {
                createTopLevelDataSet(self.dataSet);
              }
            };
          }]);

