import React from "react"
import ReactDOM from "react-dom";
import { Provider } from 'react-redux';
import { createStore,compose } from 'redux';
import { Router, Route, Link, browserHistory} from 'react-router';

import App from "./components/app.js";
import playground  from "./reducers/playground.js";

import UserManual from "./components/reading/userManual.js";
import ExploreModelsProjects from "./components/reading/exploreModelsProjects.js";
import faqs from "./components/reading/faqs.js";
import commonQueries from "./components/reading/commonQueries.js";
import apiDetails from "./components/reading/apiDetails.js";
import serverConfiguration from "./components/reading/serverConfiguration.js";
import quickstart from "./components/reading/quickstart.js";

export const PATH = "/dc-ui/"

const initialState = {
  readings: [
    {"title":"Quickstart", "path": "quickstart", "component": quickstart},
    {"title":"Playground User Manual", "path": "user-manual", "component": UserManual},
    {"title":"Explore models and projects", "path": "explore-model", "component": ExploreModelsProjects},
    {"title":"Common queries", "path": "common-queries", "component": commonQueries},
    {"title":"API details", "path": "API-details", "component": apiDetails},
    {"title":"FAQS", "path": "FAQS", "component": faqs},
    {"title":"Server configuration", "path": "server-configuration", "component": serverConfiguration},

  ],
  currentPath: "/dc/type/dcmo:model_0",
  currentJson: "",
  codeView: "classic", //classic, plainText, editable
  currentProject: "oasis.sandbox"
};


let store = createStore(playground, initialState);
window.store = store;

ReactDOM.render(
  <Provider store={store}>
    <Router history={browserHistory}>
      <Route component={App}>
        <Route path={PATH} component={UserManual}/>
        {initialState.readings.map(function(reading, i){
          return <Route path={PATH+reading.path} component={reading.component} key={reading.id}/>;
        }, this)}
      </Route>
    </Router>
  </Provider>,
  document.getElementsByClassName('generalContainer')[0]
);
