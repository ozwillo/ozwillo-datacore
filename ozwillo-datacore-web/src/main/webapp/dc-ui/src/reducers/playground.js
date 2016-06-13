
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
                plainText: action.plainText
            })

        default:
            return state
    }
}

export default playground
