import get from 'lodash/get'
import map from 'lodash/map'

const defaultColumns = {
  cells: [],
  headers: []
}

const defaultData = []

export const getColumnsFromDatasource = datasource => get(datasource, [0, 'columnsConfig'], defaultColumns)

export const getDataFromDatasource = datasource => {
    // console.log('mapModel', map(datasource, item => item.row))
    return datasource ? map(datasource, item => Object.keys(item.row).length ? item.row : null).filter(Boolean) : defaultData
}
