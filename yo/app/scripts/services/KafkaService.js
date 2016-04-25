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
                return $http.get('/api/project/' + projectId + '/topics');
              },
              /**
               * Get the details of the topic with given ID, under the given project.
               * Includes all ACLs for the topic
               * @param {type} projectId
               * @param {type} topicId
               * @returns {unresolved} A complete description of the requested topic.
               */
              getAclsForTopic: function (projectId, topicId) {
                return $http.get('/api/project/' + projectId + '/topics/' + topicId);
              },
              /**
               * Create a new Topic in the given project, of the given type. 
               * @param {type} projectId 
               * @param {type} type
               * @param {type} topicDetails The configuration of the newly created topic.
               * @returns {undefined} The newly created topic object.
               */
              createTopic: function (projectId, topicDetails) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/topic/add',
                  headers: {
                    'Content-Type': 'application/json'
                  },
                  data: topicDetails
                };
                return $http(req);
              },
              /**
               * Add a new ACL rule to a Topic in the given project. 
               * @param {type} projectId 
               * @param {type} topicId
               * @param {type} topicAcl The ACL for the topic.
               * @returns {undefined} The newly created topic object.
               */
              addAcl: function (projectId, topicId, topicAcl) {
                var req = {
                  method: 'POST',
                  url: '/api/project/' + projectId + '/topic/' + topicId + "/addAcl",
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
               * @param {type} topicId
               * @returns {undefined} true if success, false otheriwse
               */
              removeAcl: function (projectId, topicId, aclId) {
                return $http.delete('/api/project/' + projectId + '/topic/' + topicId + '/removeAcl/' + aclId);
              },
              /**
               * Delete a topic 
               * @param {type} projectId
               * @param {type} topicId
               * @returns {undefined} true if success, false otheriwse
               */
              removeTopic: function (projectId, topicId) {
                return $http.delete('/api/project/' + projectId + '/topic/' + topicId + '/removeTopic');
              },
              
              /**
               * Shares a topic with a different project.
               * @param {type} projectId
               * @param {type} topicId
               * @param {type} destProjectId
               * @returns {unresolved}
               */
              shareTopic: function (projectId, topicId, destProjectId) {
                return $http.get('/api/project/' + projectId + '/topic/' + topicId + "/share/" + destProjectId);
              },
              
              /**
               * Removes a shared topic from a project - run by the Data Owner of the project that owns the topic.
               * @param {type} projectId
               * @param {type} topicId
               * @param {type} destProjectId
               * @returns {unresolved}
               */
              unshareTopic: function (projectId, topicName, destProjectId) {
                return $http.delete('/api/project/' + projectId + '/topic/' + topicName + '/unshare/' + destProjectId);
              }
              
            };
            return service;
          }]);
