/*
 * This file is part of HopsWorks
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved.
 *
 * HopsWorks is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * HopsWorks is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with HopsWorks.  If not, see <http://www.gnu.org/licenses/>.
 */

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

