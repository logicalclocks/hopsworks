/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

/*
 * Used by home.js, main.js and project.js to set/define the elastic index 
 * where the searches will be directed to
 */
'use strict';

angular.module('hopsWorksApp')
        .service('UtilsService', function () {

          var projectName = "";
          var datasetName;

          return {
            getProjectName: function () {
              return projectName;
            },
            setProjectName: function (value) {
              //console.log("setting the parent " + value);
              projectName = value;
            },
            getDatasetName: function () {
              return datasetName;
            },
            setDatasetName: function (datasetname) {
              datasetName = datasetname;
            }
          };
        });


