import React from 'react';

export default class NavBar extends React.Component{
  componentDidMount = () => {
    $('.ui.dropdown').dropdown();
  }

  render() {
    var projects = this.props.projects.map(function(project){
      return (
        <div className="item" key={project} onClick={() => setProject(project)}>
          {project}
        </div>);
    });

    var currentProject = getProject();
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
