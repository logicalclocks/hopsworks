/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

/*
 * Used by home.js, main.js and project.js to set/define the elastic index 
 * where the searches will be directed to
 */
'use strict';

angular.module('hopsWorksApp')
        .service('UtilsService', function () {

          var index = "";
          var projectName = "";

          return {
            getIndex: function () {
              return index;
            },
            setIndex: function (value) {
              console.log("setting the index " + value);
              index = value;
            },
            getProjectName: function () {
              return projectName;
            },
            setProjectName: function (value) {
              console.log("setting the parent " + value);
              projectName = value;
            }
          };
        });


