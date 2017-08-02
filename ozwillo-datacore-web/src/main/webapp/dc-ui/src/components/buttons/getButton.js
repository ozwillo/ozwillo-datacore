import React from 'react';this
import { connect } from 'react-redux';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall} from '../../utils.js';

class GetButton extends React.Component{
  getButton = () => {
    this.props.callAPIUpdatePlayground(this.props.currentPath);
  }

  render() {
    return (
      <button className="small ui button" onClick={this.getButton}>GET</button>
    );
  }
}

const mapStateToProps = (state) => ({
  currentPath: state.currentPath
})
export default GetButton = connect(mapStateToProps)(GetButton);
