'use strict';

angular.module('hopsWorksApp')
    .factory('MessageService', function () {
        var msg = "I am writing this message to request membership in your project. My friend Jones is a member in your project from quite a long time and I have heard about the research you are doing in this project and have the desire to participate."+
            "I am writing this message to request membership in your project. My friend Jones is a member in your project from quite a long time and I have heard about the research you are doing in this project and have the desire to participate."+
            "I am writing this message to request membership in your project. My friend Jones is a member in your project from quite a long time and I have heard about the research you are doing in this project and have the desire to participate." +
            "I am writing this message to request membership in your project. My friend Jones is a member in your project from quite a long time and I have heard about the research you are doing in this project and have the desire to participate."+
            "I am writing this message to request membership in your project. My friend Jones is a member in your project from quite a long time and I have heard about the research you are doing in this project and have the desire to participate."+
            " Click here to go to the requested project <a>/project/dataset</a>";

        var dsmsg = "I am writing this message to request access to a dataset in your project." +
            " Click here to go to the requested <a>/project/dataset</a>";
        var messages = [
            {to:[{email:"ermiasg@kth.se", name:"Ermias G"}, {email:"jdowling@sics.se",name:"Jim Dowling"}],from:"Evelyn Holmes", fromEmail:"evelyn@example.com", subject:"Project join request", date:"2015-08-28T19:37:02+02:00", content:msg, unread:true},
            {to:[{email:"ermiasg@kth.se", name:"Ermias G"}], from:"Jim Dowling", fromEmail:"jdowling@sics.se", subject:"Project join request", date:"2015-08-28T19:37:02+02:00", content:msg, unread:true},
            {to:[{email:"ermiasg@kth.se", name:"Ermias G"}], from:"Jim Dowling", fromEmail:"jdowling@sics.se", subject:"DataSet access request", date:"2015-08-28T19:37:02+02:00", content:dsmsg, unread:true},
            {to:[{email:"ermiasg@kth.se", name:"Ermias G"}, {email:"admin@kth.se", name:"Admin Admin"}], from:"Jone J", fromEmail:"jone@sics.se", subject:"Project join request", date:"2015-08-28T19:37:02+02:00", content:msg, unread:true},
            {to:[{email:"ermiasg@kth.se", name:"Ermias G"}], from:"Evangelos Savvidis", fromEmail:"vangelis@kth.se", subject:"DataSet access request", date:"2015-08-28T19:37:02+02:00", content:dsmsg, unread:true},
            {to:[{email:"ermiasg@kth.se", name:"Ermias G"}], from:"Stig Viaene ", fromEmail:"stig@sics.se", subject:"Project join request", date:"2015-08-28T19:37:02+02:00", content:msg, unread:false},
            {to:[{email:"ermiasg@kth.se", name:"Ermias G"}], from:"Solomon Lemma", fromEmail:"solomon@gmail.com", subject:"DataSet access request", date:"2015-08-28T19:37:02+02:00", content:dsmsg, unread:false}
        ];
        return {
            getMessages: function () {
                return messages;
            }
        };

    });
