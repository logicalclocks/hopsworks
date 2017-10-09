'use strict';

angular.module('hopsWorksApp')
        .controller('HomeCtrl', ['ProjectService', 'UserService',
        'ModalService', 'growl', 'ActivityService', '$q', 'TourService', 
        'StorageService', '$location', '$scope',
          function (ProjectService, UserService, ModalService, growl,
          ActivityService, $q, TourService, StorageService, $location, $scope) {

            var self = this;

            self.histories = [];
            self.loadedView = false;
            self.loadedZeppelin = false;
            self.tourService = TourService;
            self.projects = [];
            self.currentPage = 1;
            self.showTours = false;
            $scope.creating = {"spark" : false, "zeppelin" : false};
            self.exampleProjectID;
            self.tours = [];
            self.tutorials = [];
            self.working = [];
            self.user = {};
            self.showTourTips = false;
            self.getTours = function () {
              self.tours = [
                {'name': 'Spark', 'tip': 'Take a tour of HopsWorks by creating a project and running a Spark job!'},
                {'name': 'Kafka', 'tip': 'Take a tour of HopsWorks by creating a project and running a Kafka job!'},
                {'name': 'TensorFlow', 'tip': 'Take a tour by creating a project and running a TensorFlow notebook!'}
//                {'name': 'Distributed TensorFlow', 'tip': 'Take a tour by creating a project and running a distributed TensorFlow Mnist job!'}
//                {'name': 'zeppelin', 'tip': 'Take a tour of Zeppelin by creating a Hopsworks project and running a Zeppelin notebook for Spark!'}
              ];
            };

            $scope.$on('$viewContentLoaded', function () {
              self.loadedView = true;
            });

            self.getTutorials = function () {
              self.tutorials = [
                {'url': 'spark.apache.org/docs/latest/running-on-yarn.html', 'description': 'Spark on YARN'},
                {'url': 'ci.apache.org/projects/flink/flink-docs-master/setup/yarn_setup.html', 'description': 'Flink on YARN'}
              ];
            };

            $scope.isCreating = function (tourName) {
              if ($scope.creating[tourName] === true) {
                return true;
              }
              return false;
            };


            // Load all projects
            var loadProjects = function (success) {
              self.projects = success;
              self.pageSizeProjects = 10;
              self.totalPagesProjects = Math.ceil(self.projects.length / self.pageSizeProjects);
              self.totalItemsProjects = self.projects.length;
              self.currentPageProjects = 1;
            };

            var loadActivity = function (success) {
              var i = 0;
              var histories = success.data;
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

              if (histories.length === 0) {
                self.histories = [];
              } else {
                histories.slice().forEach(function (history) {
                  var historyDate = new Date(history.timestamp);
                  historyDate.setHours(0, 0, 0, 0);

                  if (+historyDate === +today) {
                    if (firstToday) {
                      firstToday = false;
                      history.flag = 'today';
                    } else if (i % 10 === 0) {
                      history.flag = 'today';
                    } else {
                      history.flag = '';
                    }
                  } else if (+historyDate === +yesterday) {
                    if (firstYesterday) {
                      firstYesterday = false;
                      history.flag = 'yesterday';
                    } else if (i % 10 === 0) {
                      history.flag = 'yesterday';
                    } else {
                      history.flag = '';
                    }
                  } else if (+historyDate > +lastWeek) {
                    if (firstThisWeek) {
                      firstThisWeek = false;
                      history.flag = 'thisweek';
                    } else if (i % 10 === 0) {
                      history.flag = 'thisweek';
                    } else {
                      history.flag = '';
                    }
                  } else if (+historyDate <= +lastWeek && +historyDate >= +older) {
                    if (firstLastWeek) {
                      firstLastWeek = false;
                      history.flag = 'lastweek';
                    } else if (i % 10 === 0) {
                      history.flag = 'lastweek';
                    } else {
                      history.flag = '';
                    }
                  } else {
                    if (firstOlder) {
                      firstOlder = false;
                      history.flag = 'older';
                    } else if (i % 10 === 0) {
                      history.flag = 'older';
                    } else {
                      history.flag = '';
                    }
                  }

                  self.histories.push(history);
                  i++;

                });
              }


              self.pageSize = 10;
              self.totalPages = Math.floor(self.histories.length / self.pageSize);
              self.totalItems = self.histories.length;

            };

            var updateUIAfterChange = function (exampleProject) {

              $q.all({
                'first': ActivityService.getByUser(),
                'second': ProjectService.query().$promise
              }
              ).then(function (result) {
                if (exampleProject) {
                  self.tourService.currentStep_TourOne = 0;
                  self.tourService.KillTourOneSoon();
                }
                loadActivity(result.first);
                loadProjects(result.second);
              },
                      function (error) {
                        growl.info(error, {title: 'Info', ttl: 2000});
                      });
            };

            updateUIAfterChange(false);

            self.initCheckBox = function () {
              self.loadToursState().then(
                function(success) {
                  if (self.tourService.informAndTips || self.tourService.tipsOnly) {
                    self.showTourTips = true;
                  } else {
                    self.showTourTips = false;
                  }
                }, function(error) {
                  console.log("error");
                }
              );
            };

            self.loadToursState = function () {
                return UserService.profile().then(
                  function (success) {
                    self.user = success.data;
                    var tourState = self.user.toursState;
                    if (tourState === 0) {
                      self.tourService.setInformAndTipsState();
                    } else if (tourState === 1) {
                      self.tourService.setTipsOnlyState();
                    } else if (tourState === 2) {
                      self.tourService.setInformOnly();
                    } else if (tourState === 3) {
                      self.tourService.setShowNothingState();
                    } else {
                      self.tourService.setDefaultTourState();
                    }
                  }, function (error) {
                    growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                  });
            };

            self.disableInformBalloon = function () {
              if (self.tourService.informAndTips) {
                self.user.toursState = 1;
                self.updateProfile(self.tourService.setTipsOnlyState);
              } else if (self.tourService.informOnly) {
                self.user.toursState = 3;
                self.updateProfile(self.tourService.setShowNothingState);
              }
            };

            self.toggleTourTips = function () {
              if (self.showTourTips) {
                self.enableTourTips();
              } else {
                self.disableTourTips();
              }
              StorageService.store("hopsworks-showtourtips",self.showTourTips);
            };

            self.disableTourTips = function () {
              if (self.tourService.informAndTips) {
                self.user.toursState = 2;
                self.updateProfile(self.tourService.setInformOnly);
              } else if (self.tourService.tipsOnly) {
                self.user.toursState = 3;
                self.updateProfile(self.tourService.setShowNothingState);
              }
            };

            self.enableTourTips = function () {
              if (self.tourService.informOnly) {
                self.user.toursState = 0;
                self.updateProfile(self.tourService.setInformAndTipsState);
              } else if (self.tourService.showNothing) {
                self.user.toursState = 1;
                self.updateProfile(self.tourService.setTipsOnlyState);
              }
            };

            self.updateProfile = function (fun) {
              UserService.UpdateProfile(self.user).then (
                function (success) {
                  fun();
                }, function (error) {
                  growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                }
              );
            };

            self.showGettingStarted = function () {
              if (self.projects === undefined || self.projects === null || self.projects.length === 0) {
                return true;
              }
              return false;
            };

            // Create a new project
            self.newProject = function () {
              ModalService.createProject('lg').then(
                      function (success) {
                        updateUIAfterChange(false);
                      }, function (error) {
                growl.info("Closed project without saving.", {title: 'Info', ttl: 5000});
              });
            };

            self.createExampleProject = function (tourName) {
              $scope.creating[tourName] = true;
              if (self.showTourTips === false) {
                self.toggleTourTips();
              }
              ProjectService.example({type: tourName}).$promise.then(
                      function (success) {
                        $scope.creating[tourName] = false;
                        self.tourService.setActiveTour(tourName);
                        growl.success("Created Example Project", {title: 'Success', ttl: 5000});
                        self.exampleProjectID = success.id;
                        updateUIAfterChange(true);
                        // To make sure the new project is refreshed
//                        self.showTours = false;
                        if (success.errorMsg) {
                          $scope.creating[tourName] = false;
                          growl.warning("some problem", {title: 'Error', ttl: 10000});
                        }
                      },
                      function (error) {
                        $scope.creating[tourName] = false;
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 5000});
                      }
              );
            };

            self.deleteProjectAndDatasets = function (projectId) {
              self.working[projectId] = true;
              //Clear project StorageService state
              StorageService.remove(projectId+"-tftour-finished");
//              //Get name of project to be deleted, if it is demo_tensorflow 
//              //set anadonca disabled in TourService
//              for (var i = 0; i<self.projects.length;i++){
//                var projName = self.projects[i].project.name;
//                if(projName.startsWith("demo_tensorflow")){
//                  self.tourService.anacondaEnabled = false;
//                  break;
//                }
//              }
              ProjectService.delete({id: projectId}).$promise.then(
                      function (success) {
                        growl.success(success.successMessage, {title: 'Success', ttl: 5000});
                        updateUIAfterChange(false);
                        if (self.tourService.currentStep_TourOne > -1) {
                          self.tourService.resetTours();
                        }
                        self.working[projectId] = false;
                      },function (error) {
                        growl.error(error.data.errorMsg, {title: 'Error', ttl: 10000});
                        self.working[projectId] = false;
                      }
              );
            };

            self.EnterExampleProject = function (id) {
              $location.path('/project/' + id);
              $location.replace();
              self.tourService.resetTours();
            };
          }]);
