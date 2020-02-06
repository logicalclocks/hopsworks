/*
 * Changes to this file committed after and not including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * This file is part of Hopsworks
 * Copyright (C) 2018, Logical Clocks AB. All rights reserved
 *
 * Hopsworks is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * Hopsworks is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 *
 * Changes to this file committed before and including commit-id: ccc0d2c5f9a5ac661e60e6eaf138de7889928b8b
 * are released under the following license:
 *
 * Copyright (C) 2013 - 2018, Logical Clocks AB and RISE SICS AB. All rights reserved
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge,
 * publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS  OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR  OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

angular.module('hopsWorksApp')
        .controller('CreateTopicCtrl', ['$uibModalInstance', 'KafkaService',
        'growl', 'projectId', 'TourService', 'projectIsGuide',
          function ($uibModalInstance, KafkaService, growl, projectId,
          TourService, projectIsGuide) {

            var self = this;
            self.projectId = projectId;
            self.tourService = TourService;
            self.projectIsGuide = projectIsGuide;
            self.selectedProjectName;
            self.topicName;
            self.num_partitions;
            self.num_replicas;
            self.max_num_replicas;
            self.topicName_wrong_value = 1;
            self.replication_wrong_value = 1;
            self.topicSchema_wrong_value = 1;
            self.wrong_values = 1;
            self.working = false;
            
            self.schemas = [];
            self.subject;
            self.subjects = [];
            self.schema;
            self.schemaVersion;
            self.subjectVersions = [];
            
            self.init = function() {
                if (self.projectIsGuide) {
                  self.tourService.resetTours();
                  self.tourService.currentStep_TourFive = 0;
                }

                KafkaService.defaultTopicValues().then(
                    function (success) {
                    self.num_partitions = success.data.numOfPartitions;
                    self.num_replicas = success.data.numOfReplicas;
                    self.max_num_replicas = success.data.maxNumOfReplicas;
                }, function (error) {
                        if (typeof error.data.usrMsg !== 'undefined') {
                            growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                        } else {
                            growl.error("", {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                        }
                   });

              KafkaService.getSubjects(self.projectId).then(
                function (success) {
                  // get list of subjects
                  var data = new TextDecoder('utf-8').decode(success.data);
                  self.subjects = data.slice(1,-1).replace(/\s/g,'').split(",");
                }, function (error) {
                  if (typeof error.data.usrMsg !== 'undefined') {
                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000, referenceId: 10});
                  } else {
                    growl.error("", {title: error.data.errorMsg, ttl: 8000, referenceId: 10});
                  }
                }
              );
            };
            
            self.init();

            self.getVersionsForSubject = function(subject) {
              KafkaService.getSubjectVersions(self.projectId, subject).then(
                function(success) {
                  var data = new TextDecoder('utf-8').decode(success.data);
                  self.subjectVersions = data.slice(1,-1).replace(/\s/g,'').split(",");
                }, function (error) {
                  if (typeof error.data.usrMsg !== 'undefined') {
                    growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 8000, referenceId: 10});
                  } else {
                    growl.error("", {title: error.data.errorMsg, ttl: 8000, referenceId: 10});
                  }
                }
              )
            };

            self.guidePopulateTopic = function () {
              self.topicName = self.tourService.kafkaTopicName + "-" + self.projectId;
              self.num_partitions = 2;
              self.num_replicas = 1;

              for (var i = 0; i < self.subjects.length; i++) {
                if (angular.equals(self.subjects[i], self.tourService
                  .kafkaSchemaName + "_" + self.projectId)) {
                  self.subject = self.subjects[i];
                  break;
                }
              }

              self.schemaVersion = 1;
            };

            self.createTopic = function () {
              self.working = true;
              self.wrong_values = 1;
              self.replication_wrong_value = 1;
              self.topicName_wrong_value = 1;
              self.topicSchema_wrong_value = 1;
              
              if(self.max_num_replicas < self.num_replicas){
                self.replication_wrong_value =-1;
                self.wrong_values=-1;
              }else{
                self.replication_wrong_value =1;
              }
              //check topic name
              if(!self.topicName){
                self.topicName_wrong_value = -1;
                self.wrong_values=-1;
              }
              else{
                self.topicName_wrong_value = 1;
              }
              
              if(!self.subject || !self.schemaVersion){
                self.topicSchema_wrong_value = -1;
                self.wrong_values=-1;
              }
              else{
                self.topicSchema_wrong_value = 1;
              }
              
              if(self.wrong_values === -1){
                  self.working = false;
                  return;
              }
              
              var topicDetails ={};
              topicDetails.name=self.topicName;
              topicDetails.numOfPartitions =self.num_partitions;
              topicDetails.numOfReplicas =self.num_replicas;
              topicDetails.schemaName = self.subject;
              topicDetails.schemaVersion = self.schemaVersion;                                  

              KafkaService.createTopic(self.projectId, topicDetails).then(
                      function (success) {
                        self.working = false;
                          $uibModalInstance.close(success);
                      }, function (error) {
                      if (typeof error.data.usrMsg !== 'undefined') {
                          growl.error(error.data.usrMsg, {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                      } else {
                          growl.error("", {title: error.data.errorMsg, ttl: 5000, referenceId: 21});
                      }
                        self.working = false;
              });      
            };

            self.close = function () {
              $uibModalInstance.dismiss('cancel');
            };
          }]);
