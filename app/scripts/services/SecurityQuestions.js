'use strict';

angular.module('hopsWorksApp')
    .factory('SecurityQuestions', function () {
        var questions = ['Who is your favorite historical figure?',
                        'What is the name of your favorite teacher?',
                        'What is your first phone number?',
                        'What is the name of your favorite childhood friend?',
                        'What was the make and model of your first car?',
                        'What school did you attend for sixth grade?',
                        'In what town was your first job?'];
        return {
            getQuestions: function(){
                return questions;
            }
        };

    });
