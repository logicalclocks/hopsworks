'use strict';

angular.module('hopsWorksApp')
        .controller('HistoryCtrl', ['$scope' , 'HistoryService' , 'ModalService' ,
          function ($scope  , HistoryService , ModalService) {

            var self = this;
            self.jobs = [];
            
            $scope.searchChoices=[
                {
                    id : 0, 
                    name : "-- Please Select --", 
                    searchName: "none", 
                    values:[
                        { ind:"none" , id: "null" , type: "null" }
                    ]
                },
                {
                    id : 1, 
                    name : "Job Type", 
                    searchName: "jobType",
                    values:[
                        { ind:"jobType" , id: "null" , type: "-- Please Select --" },
                        { ind:"jobType" , id: "Spark" , type: "Spark" },
                        { ind:"jobType" , id: "Flink" , type: "Flink" },
                        { ind:"jobType" , id: "Adam" , type: "Adam" },
                        { ind:"jobType" , id: "Hadoop Java" , type: "Hadoop Java" },
                        { ind:"jobType" , id: "Map Reduce" , type: "Map Reduce" }
                    ]
                },
                {
                    id : 2, 
                    name : "Severity", 
                    searchName: "severity",
                    values:[
                        { ind:"severity" , id: "null" , type: "-- Please Select --" },
                        { ind:"severity" , id: 0 , type: "None" },
                        { ind:"severity" , id: 1 , type: "Low" },
                        { ind:"severity" , id: 2 , type: "Moderate" },
                        { ind:"severity" , id: 3 , type: "Critical" },
                        { ind:"severity" , id: 4 , type: "Severe" } 
                    ]
                },
                {
                    id : 3, 
                    name : "Score", 
                    searchName: "score",
                    values:[
                        { ind:"score" , id: "null" , type: "-- Please Select --" },
                        { ind:"score" , id: 0 , type: "0" },
                        { ind:"score" , id: 1 , type: "1" },
                        { ind:"score" , id: 2 , type: "2" },
                        { ind:"score" , id: 3 , type: "3" },
                        { ind:"score" , id: 4 , type: "4" },
                        { ind:"score" , id: 5 , type: "5" }
                    ]
                }
            ];  
            
            $scope.sortType = 'id';
            $scope.sortReverse = false;
            $scope.searchFilter;  
            
            $scope.enableSearch = false;
            
            // Attributes for the Search - Drop Down Menus
            $scope.name = '-- Please Select --';
            $scope.searchName = 'none';
            $scope.fisrtFormSelected = false;
            
            $scope.valueId = 'null';
            $scope.valueType = '-- Please Select --';
            $scope.secondFormSelected = false;
            
            $scope.sort = function (keyname) {
              $scope.sortType = keyname;   //set the sortKey to the param passed
              $scope.sortReverse = !$scope.sortReverse; //if true make it false and vice versa
            };
            
            self.showDetails = function (job) {
              ModalService.historyDetails(job , 'lg');
            };
            
            self.selectFirstForm = function (name, searchName) {
              $scope.name = name;
              $scope.searchName = searchName;
              if (searchName === "none") {
                    $scope.fisrtFormSelected = false;
                    $scope.secondFormSelected = false;
              } else {
                    $scope.fisrtFormSelected = true;
              }
            };
            
            self.selectSecondForm = function (type, id) {
              $scope.valueType = type;
              $scope.valueId = id;
              if (id === "null") {
                    $scope.secondFormSelected = false;
              } 
              else {
                    $scope.secondFormSelected = true;
              }
            };
            
            self.search = function () {

            };
            
            $scope.filterJobs = function(job){
                if($scope.searchName === 'jobType' && $scope.valueId !== 'null'){
                    return job.jobType === $scope.valueId;
                }
                else if($scope.searchName === 'severity' && $scope.valueId !== 'null'){
                    return job.severity === $scope.valueId;
                }
                else if($scope.searchName === 'score' && $scope.valueId !== 'null'){
                    return job.score === $scope.valueId;
                }
                else{
                    return job;
                }
            };
            
            var getAllHistory = function () {
              HistoryService.getAllHistoryRecords().then(
                      function (success) {
                        self.jobs = success.data;
                      });
            };
            
            getAllHistory();
            
          }]);

