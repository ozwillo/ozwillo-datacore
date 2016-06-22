
const playground = (state, action) => {
    switch (action.type) {
        case 'SET_CURRENT_QUERY_PATH':
            return Object.assign({}, state, {
                currentPath: action.currentPath
              })
        case 'SET_CURRENT_DISPLAY_RDF':
        case 'SET_CURRENT_DISPLAY':
        return {
            ...state,
            currentJson: action.currentJson,
            codeView: action.codeView
        }
        case 'SET_EDITABLE':
            return Object.assign({}, state, {
                codeView: action.codeView,
                currentJson: action.currentJson
            })
        case 'SET_CURRENT_PROJECT':
          return Object.assign({}, state, {
              currentProject: action.currentProject,
          })
        default:
            return state
    }
}

export default playground
