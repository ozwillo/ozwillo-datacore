import React from 'react';
import { connect } from 'react-redux';
import * as actions from '../actions/actionIndex.js';
import {synchronousCall, getModelFromModel} from '../utils.js';

class NavBar extends React.Component{
  componentDidMount = () => {
    $('.ui.dropdown').dropdown();
  }
  dispatchCurrentProject = (project) => {
    this.props.dispatch(actions.setCurrentProject(project));

    //we get the models of the new project
    var models;
    synchronousCall(
      "/dc/type/dcmo:model_0?dcmo:isStorage=true&dcmo:pointOfViewAbsoluteName="+project,
      (data) => {
        models = data;
      },
      (xhr, ajaxOptions, thrownError) => {
        this.setErrorMessage("Error getting this project", xhr.responseText);
      },
      {'X-Datacore-View': '-'},
      null,
      null,
      project
    );

    var display = '<a onclick="window.functionExposition.callAPIUpdatePlayground($(this).attr(\'href\'))"\
      href="/dc/type/dcmp:Project_0/'+project+'" class="dclink">'+project+'</a>\'s storage models & their stored models :<br/>';

    //we print the models
    for(var model of models){
      var modelName = getModelFromModel(model["@id"]);
      display += '- <a onclick="window.functionExposition.callAPIUpdatePlayground($(this).attr(\'href\'))"\
        href="/dc/type/dcmo:model_0/'+modelName+'" class="dclink">'+modelName+'</a>: its \
        stored <a onclick="window.functionExposition.callAPIUpdatePlayground($(this).attr(\'href\'))"\
        href="/dc/type/dcmo:model_0?dcmo:storageModel='+modelName+'" class="dclink">models</a> and\
        <a onclick="window.functionExposition.callAPIUpdatePlayground($(this).attr(\'href\'))" \
        href="/dc/type/'+modelName+'" class="dclink">all their resources</a> <br/>';
    }

    this.props.dispatch(actions.setCurrentDisplay(display));
  }

  render() {
    var parent = this;
    var projects = this.props.projects.map(function(project){
      return (
        <div className="item" key={project} onClick={() => parent.dispatchCurrentProject(project)}>
          {project}
        </div>);
    });

    var currentProject = this.props.currentProject;

    if(currentProject === null){
      currentProject = "Jeu de données";
    }

    return (
      <div className="ui inverted menu navbar centered grid darkred">
        <a className="brand item largefont">Ozwillo Datacore</a>
        <a href="/dc-ui" className="item largefont">Play</a>
        <a href="/dc-ui/import/index.html" className="item largefont">Import</a>
        <a className="ui dropdown item">{currentProject}
          <i className="dropdown icon"></i>
          <div className="menu">
            {projects}
          </div>
        </a>
      </div>
    );
  }
}

const mapStateToProps = (state/*, props*/) => ({
    currentProject: state.currentProject
})
export default NavBar = connect(mapStateToProps)(NavBar);
