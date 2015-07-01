'use strict';

angular.module('hopsWorksApp')
        .controller('HomeCtrl', ['ProjectService', 'ModalService', 'growl', 'ActivityService', 'UtilsService',
          function (ProjectService, ModalService, growl, ActivityService, UtilsService) {

            var self = this;
            self.projects = [];

            // Load all projects
            var loadProjects = function () {
              ProjectService.query().$promise.then(
                      function (success) {
                        self.projects = success;

                        self.pageSizeProjects = 10;
                        self.totalPagesProjects = Math.ceil(self.projects.length / self.pageSizeProjects);
                        self.totalItemsProjects = self.projects.length;
                        self.currentPageProjects = 1;

                      }, function (error) {
                console.log('Error: ' + error);
              }
              );
            };

            var deleteProject = function (id, wipe) {
              if (wipe) {
                ProjectService.delete({id: id}).$promise.then(
                        function (success) {
                          growl.success(success.successMessage, {title: 'Success', ttl: 15000});
                          loadProjects();
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                }
                );
              } else {
                ProjectService.remove({id: id}).$promise.then(
                        function (success) {
                          loadProjects();
                          growl.success(success.successMessage, {title: 'Success', ttl: 15000});
                        }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                }
                );
              }

            };

            //define the elastic index where the searches will be directed to
            UtilsService.setIndex("parent");

            loadProjects();
            self.histories = [];
            var histories = [];

            var loadActivity = function () {
              ActivityService.getByUser().then(
                      function (success) {
                        histories = success;
                        var today = new Date();
                        var day = today.getDate();
                        var yesterday = new Date(new Date().setDate(day - 1));
                        var lastWeek = new Date(new Date().setDate(day - 7));
                        var older = new Date(new Date().setDate(day - 14));

                        var firstToday = true;
                        var firstYesterday = true;
                        var firstThisWeek = true;
                        var firstLastWeek = true;
                        var firstOlder = true;

                        today.setHours(0, 0, 0, 0);
                        yesterday.setHours(0, 0, 0, 0);
                        lastWeek.setHours(0, 0, 0, 0);
                        older.setHours(0, 0, 0, 0);

                        var i = 0;

                        histories.data.slice().forEach(function (history) {
                          var historyDate = new Date(history.timestamp);
                          historyDate.setHours(0, 0, 0, 0);

                          if (+historyDate === +today) {
                            if (firstToday) {
                              firstToday = false;
                              history.flag = 'today';
                            } else if (i % 10 == 0) {
                              history.flag = 'today';
                            } else {
                              history.flag = '';
                            }
                          } else if (+historyDate === +yesterday) {
                            if (firstYesterday) {
                              firstYesterday = false;
                              history.flag = 'yesterday';
                            } else if (i % 10 == 0) {
                              history.flag = 'yesterday';
                            } else {
                              history.flag = '';
                            }
                          } else if (+historyDate > +lastWeek) {
                            if (firstThisWeek) {
                              firstThisWeek = false;
                              history.flag = 'thisweek';
                            } else if (i % 10 == 0) {
                              history.flag = 'thisweek';
                            } else {
                              history.flag = '';
                            }
                          } else if (+historyDate <= +lastWeek && +historyDate >= +older) {
                            if (firstLastWeek) {
                              firstLastWeek = false;
                              history.flag = 'lastweek';
                            } else if (i % 10 == 0) {
                              history.flag = 'lastweek';
                            } else {
                              history.flag = '';
                            }
                          } else {
                            if (firstOlder) {
                              firstOlder = false;
                              history.flag = 'older';
                            } else if (i % 10 == 0) {
                              history.flag = 'older';
                            } else {
                              history.flag = '';
                            }
                          }

                          self.histories.push(history);
                          i++;

                        });
                        self.pageSize = 10;
                        self.totalPages = Math.floor(self.histories.length / self.pageSize);
                        self.totalItems = self.histories.length;
                      }, function (error) {
              }
              );
            };
            loadActivity();
            self.currentPage = 1;

            // Create a new project
            self.newProject = function () {
              ModalService.createProject('lg').then(
                      function () {
                        loadProjects();
                        loadActivity();
                      }, function () {
                growl.info("Closed project without saving.", {title: 'Info', ttl: 5000});
              });
            };
            self.deleteProject = function (projectId) {
              deleteProject(projectId, false);
              loadProjects();
              loadActivity();
            };
            self.wipeProject = function (projectId) {
              deleteProject(projectId, true);
              loadProjects();
              loadActivity();
            };
          }]);


