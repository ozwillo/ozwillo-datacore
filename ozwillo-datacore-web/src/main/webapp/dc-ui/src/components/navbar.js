import React from 'react';

export default class NavBar extends React.Component{
  render() {
    return (
      <div className="ui inverted menu navbar centered grid darkred">
        <a href="" className="brand item largefont">Ozwillo Datacore</a>
        <a href="" className="item largefont">Play</a>
        <a href="" className="item largefont">Import</a>
        <a className="ui simple dropdown item">Jeu de données
          <i className="dropdown icon"></i>
          <div className="menu">
            <div className="item">Choice 1</div>
            <div className="item">Choice 2</div>
            <div className="item">Choice 3</div>
          </div>
        </a>
      </div>
    );
  }
}
