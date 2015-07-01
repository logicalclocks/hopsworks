    /*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('metaUI').controller('ElasticSearchController',
        ['$scope', 'es', 'DialogService',  function ($scope, es, DialogService) {

        var request = ejs.Request();
        
        $scope.width = 50;
        $scope.query = "";
        $scope.hits = {};
        $scope.resultsArr = [];
        $scope.additionalInfo = [];

        $scope.search = function (event) {

            //highlights the data field --- WORKS
            var highlightPost = ejs.Highlight(["data"])
                    .fragmentSize(150, "data")
                    .numberOfFragments(1, "data")
                    .preTags("<b>", "data")
                    .postTags("</b>", "data");

            // setup the request
            request
                    //.sort('id') // sort by document id -- BAD REQUEST
                    //.query(ejs.MatchAllQuery()); //WORKS
                    .query(ejs.QueryStringQuery($scope.query))
                    .highlight(highlightPost);

            var restQry = JSON.stringify(request, null, 4);
            console.log("searching request " + restQry);

            $scope.results = es.search({
                index: 'hdfs_metadata',
                body: request
            })
            .then(function (body) {
                $scope.hits = body.hits;
                $scope.resultsArr.push($scope.results);
                console.log(JSON.stringify($scope.hits));
            }, function (error) {
                console.trace(error.message);
            });
        };

        $scope.searchOnKeyEvent = function(keyevent){
            if(keyevent.keyCode === 13){
                this.search();
            }
        };
        
        $scope.findAll = function () {
            request.query(ejs.MatchAllQuery());

            $scope.results = es.search({
                index: 'hdfs_metadata',
                body: request
            })
            .then(function (body) {
                $scope.hits = body.hits;
            }, function (error) {
                console.trace(error.message);
            });

        };

        $scope.renderResult = function (result) {
            // console.log(result);
            var resultText = "";
            if (result.highlight)
                resultText = result.highlight.data[0];
            else if (result._source.data)
                resultText = result._source.data;
            else
                resultText = result._id;

            return resultText;
        };

        $scope.display = function(data, index){
            //console.log("searchable " + JSON.stringify(data._source.searchable));
            
            if(data._source.searchable === 0){
                var content = {header:'forbidden', body: 'You cannot access this information'};
                DialogService.launch('error', content);
            }else{
                $scope.additionalInfo = [];
                $scope.additionalInfo[index] = "<a href>inodeid " + data._source.inodeid + "</a>";
            }
        };
        
        $scope.search();
    }]);