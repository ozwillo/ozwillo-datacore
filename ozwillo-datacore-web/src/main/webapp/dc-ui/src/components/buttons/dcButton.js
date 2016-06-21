import React from 'react';
import { connect } from 'react-redux';

import * as actions from '../../actions/actionIndex.js';
import {ajaxCall} from '../../utils.js';

class DCButton extends React.Component{

  extractDc = (resource) => {
    return {
      "@id": resource["@id"],
      "o:version": resource["o:version"],
      "@type": resource["@type"],
      "dc:created": resource["dc:created"],
      "dc:modified": resource["dc:modified"],
      "dc:creator": resource["dc:creator"],
      "dc:contributor": resource["dc:contributor"],
      "o:partial": resource["o:partial"],
    };
  }

  dcButton = () => {
    var relativeUrl = this.props.currentPath;
    ajaxCall(
      relativeUrl,
      (data) => {
        if (Object.prototype.toString.call( data ) === '[object Array]' ){
          var list_data = [];
          for (var resource of data){
            list_data.push(this.extractDc(resource));
          }
          var resResourcesOrText = displayJsonListResult(list_data, relativeUrl);
        }
        else{
          data = this.extractDc(data);
          var resResourcesOrText = displayJsonObjectResult(data);
        }
        this.props.dispatch(actions.setCurrentDisplay(resResourcesOrText));
      },
      (xhr) => {
        this.props.setErrorMessage("Error accessing the current path", xhr.responseText);
      }
    );
  }

  render() {
    return (
      <button className="small ui button" onClick={this.dcButton} id="dcButton" data-content="List view (Dublin Core Notation)">dc</button>
    );
  }
}

const mapStateToProps = (state) => ({
  currentPath: state.currentPath
})
export default DCButton = connect(mapStateToProps)(DCButton);
