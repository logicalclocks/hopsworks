'use strict';

angular.module('hopsWorksApp')
  .controller('HomeCtrl', ['ProjectService', 'ModalService', 'growl', 'ProjectHistoryService',
    function (ProjectService, ModalService, growl, ProjectHistoryService) {

      var self = this;
      self.projects = [];
      self.histories= {"today":[],
                       "yesterday":[],
                       "thisWeek":[],
                       "lastWeek":[],
                       "older":[]};
        var timeLine = function (histories) {
            var today = new Date();
            var day = today.getDate();
            var yesterday = new Date(new Date().setDate(day - 1));
            var lastWeek = new Date(new Date().setDate(day - 7));
            var older = new Date(new Date().setDate(day - 14));
            today.setHours(0,0,0,0);
            yesterday.setHours(0,0,0,0);
            lastWeek.setHours(0,0,0,0);
            older.setHours(0,0,0,0);
            histories.data.forEach( function(history) {
                var historyDate = new Date(history.datestamp);
                historyDate.setHours(0,0,0,0);
                if (+historyDate === +today){
                    self.histories.today.push(history);
                } else if (+historyDate === +yesterday) {
                    self.histories.yesterday.push(history);
                } else if (+historyDate > +lastWeek) {
                    self.histories.thisWeek.push(history);
                } else if (+historyDate <= +lastWeek && +historyDate >= +older) {
                    self.histories.lastWeek.push(history);
                } else {
                    self.histories.older.push(history);
                }

            });
            return timeLines;
        }
      var histories = [];
      ProjectHistoryService.getByUser().then(
          function(success) {
              histories = success;
              timeLine(histories);
              console.log(self.histories);
          }, function (error){
              console.log('Error: ' + error);
          }
      );

      // Load all projects
      ProjectService.query().$promise.then(
        function(success) {
          self.projects = success;
        }, function (error){
          console.log('Error: ' + error);
        }
      );

      self.newProject = function () {
        ModalService.createProject('lg', 'New project', '').then(
          function (success) {
            self.projects = ProjectService.query();
            growl.success("Successfully created project: " + success.name, {title: 'Success', ttl: 5000});
          }, function () {
            growl.info("Closed project without saving.", {title: 'Info', ttl: 5000});
          });
      };

    }]);

