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
        .controller('SchemaViewContentCtrl', ['$uibModalInstance', '$scope', 'KafkaService', 'growl', 'projectId', 'schemaName', 'schemaVersion',
          function ($uibModalInstance, $scope, KafkaService, growl, projectId, schemaName, schemaVersion) {

            var self = this;
            
            self.projectId = projectId;
            self.schemaVersion = schemaVersion;
            self.schemaContents =[];
            self.schemaContent;
            $scope.jsonObj = "";
            
            
            self.init = function() {
                   
              KafkaService.getSchemaContent(self.projectId, schemaName, self.schemaVersion).then(
                 function (success) {
                 self.schemaContents = success.data;
                 $scope.jsonObj = success.data.contents;
                 }, function (error) {
                 growl.error(error.data.errorMsg, {title: 'Could not get schema for topic', ttl: 5000, referenceId: 21});
                 });
              
             
            };
            
            self.init();            

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);

