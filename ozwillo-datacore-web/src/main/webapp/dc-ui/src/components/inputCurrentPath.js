import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../actions/actionIndex.js';

class InputCurrentPath extends React.Component{
  updateCurrentPath = (e) => {
    this.props.dispatch(actions.setCurrentQueryPath(e.target.value));
  }
  render() {
    return (
      <input className="myurl" value={this.props.currentPath} type="text" onChange={this.updateCurrentPath}/>
    );
  }
}
const mapStateToPropsInputCurrentPath = (state/*, props*/) => ({
    currentPath: decodeURIComponent(state.currentPath)
})
export default InputCurrentPath = connect(mapStateToPropsInputCurrentPath)(InputCurrentPath);
