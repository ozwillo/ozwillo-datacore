import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../actions/actionIndex.js';

export default class Reading extends React.Component{
  componentWillUpdate = (nextProps) => {
    //we make the animation only if we have a new reading
    if(nextProps.reading !== this.props.reading){
      $('.reading').transition('hide');
      $('.reading').transition('fade right');
    }
  }
  render() {
    return (
      <div className="row ui segment reading segmentpadding">
        {/*we pass the function to children in this particular way because of this.props.reading, wich is waiting for a function as props*/}
        {React.cloneElement(this.props.reading, { callAPIUpdatePlaygroundOnClick: this.props.callAPIUpdatePlaygroundOnClick, tools: this.props.tools })}
      </div>
    );
  }
}
