/*jshint undef: false, unused: false, indent: 2*/
/*global angular: false */

'use strict';
function Board(name, numberOfColumns) {
  return {
    name: name,
    numberOfColumns: numberOfColumns,
    columns: [],
    backlogs: []
  };
}

function Column(id, name) {
  return {
    id: id,
    name: name,
    cards: []
  };
}

function Backlog(name) {
  return {
    name: name,
    phases: []
  };
}

function Phase(name) {
  return {
    name: name,
    cards: []
  };
}

function Card(id, title, list, details, editing,
        find, required, sizefield, description, fieldtypeid, fieldtypeContent) {
  this.id = id;
  this.title = title;
  this.list = list;
  this.details = details;
  this.editing = editing;
  this.find = find;
  this.required = required;
  this.sizefield = sizefield;
  this.description = description;
  this.fieldtypeid = fieldtypeid;
  this.fieldtypeContent = fieldtypeContent;
  return this;
}
