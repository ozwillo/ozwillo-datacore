import React from 'react';
import { connect } from 'react-redux'
import {Link} from 'react-router';
import {PATH} from '../index.js'


export default class Menu  extends React.Component{
  constructor(props){
    super(props);
    this.state= {
      hidden: true,
      onButton: false
    };
  }

  componentDidMount(){
    $('.ui.mymenu').hide();

    $('.menubutton').dropdown({
     onChange: function(val) {
       $('.menubutton')
         .dropdown('set text', "Menu")
       ;
      }
    });
  }

  render(){
    return (
      <div className="ui four wide column centered grid">
        <div className="ui center sticky groupementmenu">
          <div className="ui center column white dropdown big launch blue basic button menubutton center aligned container">
            <i className="content icon"></i>
            <span className="text">Menu</span>
            <ConnectedMenuDepliant/>
          </div>
        </div>
      </div>
    );
  }
}

class MenuDepliant extends React.Component{
  render(){
    return(
      <div className="ui myCentering hidden vertical compact menu mymenu">
        {this.props.menuItems.map((menuItem, i) => {
          return <ConnectedMenuElement ElementMenu={menuItem} key={i}/>;
        }, this)}
      </div>
    );
  }
}

const mapStateToPropsMenuDepliant = (state/*, props*/) => {
  return {
    menuItems: state.readings
  }
};
const ConnectedMenuDepliant = connect(mapStateToPropsMenuDepliant)(MenuDepliant);


class MenuElement extends React.Component{
  render(){
    return(
      <Link className="item" to={PATH+this.props.ElementMenu.path}>{this.props.ElementMenu.title}</Link>
    );
  }
}

const ConnectedMenuElement = connect()(MenuElement);
