import React from 'react';
import { connect } from 'react-redux'

export default class LinkPlayground extends React.Component{

  render(){
    return(
      <a className="playground" onClick={this.props.tools().callAPIUpdatePlaygroundOnClick} href={this.props.url}>{this.props.value}</a>
    );
  }
}
