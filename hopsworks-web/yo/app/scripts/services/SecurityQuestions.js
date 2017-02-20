'use strict';

angular.module('hopsWorksApp')
        .factory('SecurityQuestions', function () {
          var questions = ["Mother's maiden name?",
            'Name of your first pet?',
            'Name of your first love?'];
          return {
            getQuestions: function () {
              return questions;
            }
          };

        });
