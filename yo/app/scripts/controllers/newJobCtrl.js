/**
 * Created by stig on 2015-07-27.
 * Controller for the jobs page.
 */

'use strict';

angular.module('hopsWorksApp')
        .controller('NewJobCtrl', ['$routeParams', 'growl', 'JobService', '$location',
          function ($routeParams, growl, JobService,$location) {

            var self = this;
            this.projectId = $routeParams.projectID;
            this.jobtype; //Will hold the selection of which job to create.
            
            this.createJob = function(type, config){
              JobService.createNewJob(self.projectId, type, config).then(
                      function (success) {
                        $location.path('project/' + self.projectId + '/jobs');
                      }, function (error) {
                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
              });
            }


          }]);


