'use strict';
angular.module('hopsWorksApp')
        .controller('SearchResultCtrl', ['DelaService', '$routeParams', '$scope', '$rootScope', '$interval',
          function (DelaService, $routeParams, $scope, $rootScope, $interval) {
            var self = this;
            self.userContents = [];
            self.projectId = parseInt($routeParams.projectID, 10);
            self.results = [];
            var dataset = false;
            
            self.init = function(results) {
              self.results = results;
              getUserContents();
            };
            
            self.init_ds = function(results) {
              dataset = true;
              self.results = results;
              getUserContents();
            };
            
            var getUserContents = function () {
              if (!$rootScope.isDelaEnabled) {
                return;
              }
              var elementSummaries = [];
              if (dataset) {
                DelaService.getUserContents().then(function (success) {
                  self.userContents = success.data;
                  elementSummaries = getElementSummary();
                  if (self.results.files.length > 0 && self.userContents.length > 0) {
                    self.results.files.forEach(function (entry) {
                      checkProgress(entry, elementSummaries);
                    });
                  }
                }, function (error) {
                  console.log("Error getting user contents: ", error);
                });
              } else {
                DelaService.getUserContents().then(function (success) {
                  self.userContents = success.data;
                  elementSummaries = getElementSummary();
                  if (self.results.length > 0 && self.userContents.length > 0) {
                    self.results.forEach(function (entry) {
                      checkProgress(entry, elementSummaries);
                    });
                  }
                }, function (error) {
                  console.log("Error getting user contents: ", error);
                });
              };

            };

            var getUserContentsInterval;
            if ($rootScope.isDelaEnabled) {
              getUserContentsInterval = $interval(function () {
                getUserContents();
              }, 10000);
            }

            var getElementSummary = function () {
              var elementSummaries = [];
              //if we have a project id look only for summaries in a project
              if (self.projectId) {
                self.userContents.forEach(function (entry) {
                  if (self.projectId === entry.projectId) {
                    elementSummaries = entry.elementSummaries;
                  }
                });
              } else {
                self.userContents.forEach(function (entry) {
                  elementSummaries = elementSummaries.concat(entry.elementSummaries);
                });
              } 
              return elementSummaries;
            };
            
            var checkProgress = function (ds, elementSummaries) {
              var torrentId = ds.publicId;
              if (torrentId === undefined) {
                return;
              }

              elementSummaries.forEach(function(entry) {
                if (torrentId === entry.torrentId.val && 
                    (entry.torrentStatus === "DOWNLOADING" || 
                     entry.torrentStatus === "PREPARE_DOWNLOAD")) {
                  ds['downloading'] = true;
                  return;
                } else if (torrentId === entry.torrentId.val) {
                  ds['downloading'] = false;
                  ds['downloaded'] = true;
                  return;
                } else {
                  ds['downloading'] = false;
                  ds['downloaded'] = false;
                }
              });
              return;
            };

            $scope.$on("$destroy", function () {
              console.log("Destroy getUserContentsInterval.");
              if(getUserContentsInterval) {
                $interval.cancel(getUserContentsInterval);
              }
            });
          }]);


