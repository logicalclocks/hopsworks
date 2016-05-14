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
