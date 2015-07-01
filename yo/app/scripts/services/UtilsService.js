/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';

angular.module('hopsWorksApp')
        .service('UtilsService', function () {
                
                var index = "";
                var projectName = "";
                
                return {
                    getIndex: function(){
                        return index;
                    },
                    
                    setIndex: function(value){
                        console.log("setting the index " + value);
                        index = value;
                    },
                    
                    getProjectName: function(){
                        return projectName;
                    },
                    
                    setProjectName: function(value){
                        console.log("setting the parent " + value);
                        projectName = value;
                    }
                };  
});


