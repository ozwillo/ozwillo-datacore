import React from 'react';
import { connect } from 'react-redux'

export default class LinkPlayground extends React.Component{
  handleClick = (event) => {
    var tools = this.props.tools();
    tools.callAPIUpdatePlaygroundOnClick(event);
    tools.goToTop();
  }
  render(){
    return(
      <a className="playground" onClick={this.handleClick} href={this.props.url}>{this.props.value}</a>
    );
  }
}
