'use strict';

angular.module('hopsWorksApp')
        .factory('SecurityQuestions', function () {
          var questions = ['Who is your favorite historical figure?',
            'What is the name of your favorite teacher?',
            'What is your first phone number?',
            'What is the name of your favorite childhood friend?'];
          return {
            getQuestions: function () {
              return questions;
            }
          };

        });
