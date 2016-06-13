export function setCurrentReading(currentReading) {
  return {
    type: 'SET_CURRENT_READING',
    currentReading: currentReading
  }
}

export function setCurrentQueryPath(currentPath) {
  return {
    type: 'SET_CURRENT_QUERY_PATH',
    currentPath: currentPath
  }
}

export function setCurrentDisplay(currentJson) {
  return {
    type: 'SET_CURRENT_DISPLAY',
    currentJson: currentJson,
    RDF: false
  }
}

export function setCurrentDisplayRDF(currentJson) {
  return {
    type: 'SET_CURRENT_DISPLAY_RDF',
    currentJson: currentJson,
    RDF: true
  }
}
