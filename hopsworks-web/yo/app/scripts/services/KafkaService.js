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

'use strict';
/*
 * Service allowing fetching topic history objects by type.
 */
angular.module('hopsWorksApp')

        .factory('KafkaService', ['$http', function ($http) {
            var service = {
              /**
               * Get all the topics defined in the project with given id.
               * @param {int} projectId
               * @returns {unresolved} A list of topic objects.
               */
              getTopics: function (projectId) {
                return $http.get('/api/project/' + projectId + '/kafka/topics');
              },
              /**
               * Get all the topics defined in the project with given id.
               * @param {int} projectId
               * @returns {unresolved} A list of topic objects.
               */
              getSharedTopics: function (projectId) {
                return $http.get('/api/project/' + projectId + '/kafka/sharedTopics');
              },
              /**
               * Get all the topics defined in the project with given id.
               * @param {int} projectId
               * @returns {unresolved} A list of topic objects.
               */
              getProjectAndSharedTopics: function (projectId) {
                return $http.get('/api/project/' + projectId  + '/kafka/projectAndSharedTopics');
              },
              /**
               * Get the details of the topic with given ID, under the given project.
               * Includes all description for the topic
               * @param {type} projectId
               * @param {type} topicName
               * @returns {unresolved} A complete description of the requested topic.
               */
              getTopicDetails: function (projectId, topicName) {
                return $http.get('/api/project/' + projectId + '/kafka/details/' + topicName);
              },
              
              /**
               * Get the details of the topic with given ID, under the given project.
               * Includes all ACLs for the topic
               * @param {type} projectId
               * @param {type} topicName
               * @returns {unresolved} A complete description of the requested topic.
               */
              getAclsForTopic: function (projectId, topicName) {
                return $http.get('/api/project/' + projectId + '/kafka/acls/' + topicName);
              },
              
              defaultTopicValues: function (projectId){
                return $http.get('/api/project/' + projectId + '/kafka/topic/defaultValues');
              },
              
              validateSchema: function (projectId, schemaDetails){
                  var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/kafka/schema/validate',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: schemaDetails
                };
                return $http(req);
              },
              
              /**
               * Create a new schema for topics in the given project, of the given type. 
               * @param {type} projectId
               * @param {type} schemaDetails The configuration of the newly created topic.
               * @returns {undefined} The newly created topic object.
               */
              createSchema: function (projectId, schemaDetails) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/kafka/schema/add',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: schemaDetails
                };
                return $http(req);
              },
              
              getSchemasForTopics: function (projectId){
                return $http.get('/api/project/' + projectId + '/kafka/schemas');
              },
              
              getSchemaContent: function (projectId, schemaName, schemaVersion){
                return $http.get('/api/project/' + projectId + '/kafka/showSchema/'+schemaName+'/'+schemaVersion);
              },
              
              deleteSchema: function(projectId, schemaName, schemaVersion){
                    return $http.delete('/api/project/' + projectId + '/kafka/removeSchema/'+schemaName+'/'+schemaVersion);
              },
              
              /**
               * Create a new Topic in the given project, of the given type. 
               * @param {type} projectId 
               * @param {type} topicDetails The configuration of the newly created topic.
               * @returns {undefined} The newly created topic object.
               */
              createTopic: function (projectId, topicDetails) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/kafka/topic/add',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: topicDetails
                };
                return $http(req);
              },

              aclUsers: function(projectId, topicName){
                return $http.get('/api/project/' + projectId + '/kafka/aclUsers/topic/' + topicName);
              },
              
              /**
               * Add a new ACL rule to a Topic in the given project. 
               * @param {type} projectId 
               * @param {type} topicName
               * @param {type} topicAcl The ACL for the topic.
               * @returns {undefined} The newly created topic object.
               */
              createTopicAcl: function (projectId, topicName, topicAcl) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/kafka/topic/' + topicName + "/addAcl",
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: topicAcl
                };
                return $http(req);
              },
              /**
               * Delete an ACL rule for a topic
               * @param {type} projectId
               * @param {type} topicName
               * @returns {undefined} true if success, false otheriwse
               */
              removeTopicAcl: function (projectId, topicName, aclId) {
                return $http.delete('/api/project/' + projectId + '/kafka/topic/' + topicName + '/removeAcl/' + aclId);
              },
              
              updateTopicAcl: function(projectId, topicName, aclId, topicAcl){                
                var req = {
                  method: 'PUT',
                  url: '/api/project/' + projectId + '/kafka/topic/' + topicName + '/updateAcl/' + aclId,
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: topicAcl
                  };
                return $http(req);  
              },
              /**
               * Delete a topic 
               * @param {type} projectId
               * @param {type} topicName
               * @returns {undefined} true if success, false otheriwse
               */
              removeTopic: function (projectId, topicName) {
                return $http.delete('/api/project/' + projectId + '/kafka/topic/' + topicName + '/remove');
              },
              
              /**
               * Shares a topic with a different project.
               * @param {type} projectId
               * @param {type} topicName
               * @param {type} destProjectId
               * @returns {unresolved}
               */
              shareTopic: function (projectId, topicName, destProjectId) {
                return $http.get('/api/project/' + projectId + '/kafka/topic/' + topicName + "/share/" + destProjectId);
              },
              
              /**
               * Removes a shared topic from a project - run by the Data Owner of the project that owns the topic.
               * @param {type} projectId
               * @param {type} topicName
               * @param {type} destProjectId
               * @returns {unresolved}
               */
              unshareTopic: function (projectId, topicName, destProjectId) {
                return $http.delete('/api/project/' + projectId + '/kafka/topic/' + topicName + '/unshare/' + destProjectId);
              },
              
              unshareTopicFromProject: function (projectId, topicName) {
                return $http.delete('/api/project/' + projectId + '/kafka/topic/' + topicName + '/unshare/');
              },
              
              topicIsSharedTo: function (projectId, topicName){
                  return $http.get('/api/project/' + projectId + '/kafka/'+topicName+'/sharedwith');
              }
              
            };
            return service;
          }]);
