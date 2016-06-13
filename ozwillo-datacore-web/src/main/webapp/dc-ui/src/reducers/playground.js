
const playground = (state, action) => {
    switch (action.type) {
        case 'SET_CURRENT_QUERY_PATH':
            return Object.assign({}, state, {
                currentPath: action.currentPath
              })
        case 'SET_CURRENT_DISPLAY_RDF':
        case 'SET_CURRENT_DISPLAY':
            return Object.assign({}, state, {
                currentJson: action.currentJson,
                codeView: action.codeView,

            })
        case 'SET_EDITABLE':
            return Object.assign({}, state, {
                codeView: action.codeView,
                currentJson: action.currentJson
            })
        default:
            return state
    }
}

export default playground
