import React from 'react';

import Menu from "./menu.js";
import NavBar from "./navbar.js";
import Content from "./content.js";


export default class App extends React.Component{
  render() {
    return (
      <div>
        <NavBar/>
        <div className="ui container stackable grid">
          <Menu/>
          <Content reading={this.props.children}/>
        </div>
      </div>
    );
  }
}
