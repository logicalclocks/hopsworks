/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

var mainModule = angular.module('metaUI').controller('MetadataViewController',
        ['$scope', '$modalInstance', 'BoardService',
            function ($scope, $modalInstance, BoardService) {

                var rawdata = BoardService.getMetadata();

                $scope.close = function () {
                    $modalInstance.close();
                };

                var results = {headers: [], values: []};
                
                //transpose the metadata array
                angular.forEach(rawdata.fields, function (value, key) {
                    results.headers.push(value.name);

                    angular.forEach(value.data, function (inner, index) {
                        results.values[index] = results.values[index] || [];
                        results.values[index].push(inner);
                    });
                });

                $scope.tableName = rawdata.table;
                $scope.metadata = results;
                console.log("ARRAY " + JSON.stringify(results));
            }
        ]);



