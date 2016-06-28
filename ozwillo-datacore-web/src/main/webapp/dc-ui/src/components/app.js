import React from 'react';
import { connect } from 'react-redux';

import Menu from "./menu.js";
import NavBar from "./navbar.js";
import Content from "./content.js";


class App extends React.Component{
  constructor(props) {
     super(props);
     //we expose this in order to access it from datacore-ui.js
     window.getCurrentProject = this.getCurrentProject;
  }

  getCurrentProject = () => {
    return this.props.currentProject;
  }

  componentDidMount = () => {
    $('.ui.sticky')
      .sticky({
        context: '#mainContainer'
      })
    ;

    $(window).scroll(function () {
      if ($(this).scrollTop() > 150) {
              $('.goTop').addClass('show');
          } else {
              $('.goTop').removeClass('show');
          }
    });
  }

  goToTop = () => {
    $("html, body").animate({
        scrollTop: 0
    }, 600);
  }

  render() {
    return (
      <div>
        <NavBar projects={getAllProjects()}/>
        <div className="ui container stackable grid" id="mainContainer">
          <Menu/>
          <Content reading={this.props.children} goToTop={this.goToTop}/>

          <div className="ui icon button goTop" onClick={this.goToTop}>
            <i className="arrow up icon centered"></i>
          </div>
        </div>
      </div>
    );
  }
}

const mapStateToProps = (state/*, props*/) => ({
    currentProject: state.currentProject
})
export default App = connect(mapStateToProps)(App);
