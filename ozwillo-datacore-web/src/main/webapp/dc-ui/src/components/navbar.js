import React from 'react';
import { connect } from 'react-redux';
import * as actions from '../actions/actionIndex.js';

class NavBar extends React.Component{
  componentDidMount = () => {
    $('.ui.dropdown').dropdown();
  }
  dispatchCurrentProject = (project) => {
    this.props.dispatch(actions.setCurrentProject(project));
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
