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

angular.module('hopsWorksApp')
        .controller('SchemaUpdateContentCtrl', ['$uibModalInstance', '$scope', 'KafkaService', 'growl', 'projectId', 'schemaName', 'schemaVersion',
            function ($uibModalInstance, $scope, KafkaService, growl, projectId, schemaName, schemaVersion) {

                var self = this;
                self.projectId = projectId;
                self.schemaName = schemaName;
                self.contents;
                self.schemaVersion = schemaVersion;
                self.content_empty = 1;
                self.message ="";
                self.validSchema = "invalid";

                self.init = function () {

                    KafkaService.getSchemaContent(self.projectId, schemaName, self.schemaVersion).then(
                            function (success) {
                                $scope.jsonObj = success.data.contents;
                            }, function (error) {
                        growl.error(error.data.errorMsg, {title: 'Could not get schema for topic', ttl: 5000, referenceId: 21});
                    });
                };

                self.init();
                
                self.validateSchema = function () {
                   
                    self.validSchema = "invalid";

                    if(!self.contents){
                        self.content_empty = -1;
                        self.wrong_values = -1;
                    }

                    if(self.wrong_values === -1){
                        return;
                    }

                    var schemaDetail ={};
                    schemaDetail.name=self.schemaName;
                    schemaDetail.contents =self.contents;
                    //schemaDetail.version =self.version;
                    schemaDetail.versions =[];

                    KafkaService.validateSchema(self.projectId, schemaDetail).then(
                            function (success) {
                                self.message = "schema is valid";
                                self.validSchema="";
                            }, function (error) {
                                self.message = error.data.errorMsg;
                    });
                 };
                
                self.createSchema = function () {                   
                    
                    var schemaDetail = {};
                    schemaDetail.name = schemaName;
                    schemaDetail.contents =self.contents;
                    schemaDetail.version = self.schemaVersion + 1;

                    KafkaService.createSchema(self.projectId, schemaDetail).then(
                            function (success) {
                                $uibModalInstance.close(success);
                            }, function (error) {
                                 self.message = error.data.errorMsg;
                                 self.validSchema="invalid";
                    });
                };
                
                $scope.doWith = function (newJson) {
                  self.content_empty = 1;
                  self.wrong_values = 1;
                  self.contents = JSON.stringify(newJson, null, 2);
                };

                self.close = function () {
                    $uibModalInstance.dismiss('cancel');
                };
            }]);