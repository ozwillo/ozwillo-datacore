import React from 'react';
import { connect } from 'react-redux'
import {Link} from 'react-router';

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
  }

  //this syntax makes an autobind of this function
  enterButton = () => {
    if(this.state.hidden == true){
      $('.ui.mymenu').transition('fade right');
      this.setState({hidden: false, onButton: true});
    }
  }

  outMenu = () => {
    if(this.state.hidden == false){
      $('.ui.mymenu ').transition('fade right');
      this.setState({hidden: true});
    }
  }

  render(){
    return (
      <div className="four wide column center aligned groupementmenu" onMouseLeave={this.outMenu}>
        <div className="ui white big launch button menubutton" onMouseEnter={this.enterButton}>
          <i className="content icon"></i>
          <span className="text">Menu</span>
        </div>
        <ConnectedMenuDepliant/>
      </div>
    );
  }
}

class MenuDepliant extends React.Component{
  render(){
    return(
      <div className="ui hidden vertical compact menu mymenu">
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
      <Link className="item" to={this.props.ElementMenu.path}>{this.props.ElementMenu.title}</Link>
    );
  }
}

const ConnectedMenuElement = connect()(MenuElement);
