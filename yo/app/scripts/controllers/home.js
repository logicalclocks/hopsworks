'use strict';

angular.module('hopsWorksApp')
        .controller('HomeCtrl', ['ProjectService', 'ModalService', 'growl', 'ActivityService', 'UtilsService', '$q', 'TourService', '$location', '$scope',
            function (ProjectService,  ModalService, growl, ActivityService, UtilsService, $q, TourService, $location, $scope) {

                var self = this;

                self.histories = [];
                self.loadedView = false;
                self.tourService = TourService;
                self.projects = [];
                self.currentPage = 1;
                $scope.showTours = false;
                $scope.showTutorials = false;
                $scope.showPublicDatasets = false;
                self.creating = false;
                self.exampleProjectID;
                self.tours = [{ 'name': 'Starting Spark', 'tip': 'Take a tour of Hopsworks by creating a project and running a Spark job!'}];
                self.tutorials = ['Spark Intro', 'Flink Intro'];
                self.publicDatasets = [];
                // Load all projects

                var loadProjects = function (success) {
                    self.projects = success;
                    self.pageSizeProjects = 10;
                    self.totalPagesProjects = Math.ceil(self.projects.length / self.pageSizeProjects);
                    self.totalItemsProjects = self.projects.length;
                    self.currentPageProjects = 1;
                };

                $scope.$on('$viewContentLoaded', function () {
                    self.loadedView = true;
                });

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
                                growl.info(error, {title: 'Info', ttl: 5000});
                            });


                };


                updateUIAfterChange(false);



                self.addPublicDatasetModal = function (inodeId, name, description) {

                  ProjectService.getDatasetInfo({inodeId: inodeId}).$promise.then(
                                function (response) {
                                    var datasetDto = response.data;
                                    var projects;
                                    //fetch the projects to pass them in the modal. 
                                    ProjectService.query().$promise.then(
                                            function (success) {
                                                projects = success;

                                                //show dataset
                                                ModalService.viewPublicDataset('md', projects, datasetDto)
                                                        .then(function (success) {
                                                            growl.success(success.data.successMessage, {title: 'Success', ttl: 1000});
                                                        }, function (error) {

                                                        });
                                            }, function (error) {
                                        console.log('Error: ' + error);
                                    });

                                }, function (error) {
                            growl.error(error.data.errorMsg, {title: 'Error', ttl: 10000});
                        });
                  
                
                };
                
                
                
                self.getPublicDatasets = function () {
                    ProjectService.getPublicDatasets().$promise.then(
                            function (success) {
                              self.publicDatasets = success;
                            },
                            function (error) {
                                self.creating = false;
                                growl.info("Could not load Public Datasets", {title: 'Info', ttl: 5000});
                            }

                    );
                };
                
                self.getTours = function () {
                  self.availableTours = ['spark'];
                };

                self.getTutorials = function () {
                  self.availableTutorials = ['zeppelin'];
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
                self.createExampleProject = function () {
                    self.creating = true;
                    ProjectService.example().$promise.then(
                            function (success) {
                                self.creating = false;
                                growl.success("Created Example Project", {title: 'Success', ttl: 10000});
                                self.exampleProjectID = success.id;
                                updateUIAfterChange(true);
                                if (success.errorMsg) {
                                    self.creating = false;
                                    growl.warning("some problem", {title: 'Error', ttl: 10000});
                                }

                            },
                            function (error) {
                                self.creating = false;
                                growl.info("problem", {title: 'Info', ttl: 5000});
                            }

                    );
                };

                self.deleteOnlyProject = function (projectId) {
                    ProjectService.remove({id: projectId}).$promise.then(
                            function (success) {
                                growl.success(success.successMessage, {title: 'Success', ttl: 15000});
                                updateUIAfterChange(false);
                                if(self.tourService.currentStep_TourOne > -1){
                                    self.tourService.resetTours();
                                }
                            },
                            function (error) {
                                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                            }
                    );
                };
                self.deleteProjectAndDatasets = function (projectId) {

                    ProjectService.delete({id: projectId}).$promise.then(
                            function (success) {
                                growl.success(success.successMessage, {title: 'Success', ttl: 15000});
                                updateUIAfterChange(false);
                                if(self.tourService.currentStep_TourOne > -1){
                                    self.tourService.resetTours();
                                }
                            },
                            function (error) {
                                growl.error(error.data.errorMsg, {title: 'Error', ttl: 15000});
                            }
                    );
                };

                self.EnterExampleProject = function (id) {
                    $location.path('/project/' + id);
                    self.tourService.resetTours();
                };
            }]);


