import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../actions/actionIndex.js';

export default class Reading extends React.Component{
  componentWillUpdate = () => {
    $('.reading').transition('hide');
    $('.reading').transition('fade right');
  }
  render() {
    return (
      <div className="row ui segment reading segmentpadding">
        {/*we pass the function to children in this particular way because of this.props.reading*/}
        {React.cloneElement(this.props.reading, { callAPIUpdatePlaygroundOnClick: this.props.callAPIUpdatePlaygroundOnClick })}
      </div>
    );
  }
}
