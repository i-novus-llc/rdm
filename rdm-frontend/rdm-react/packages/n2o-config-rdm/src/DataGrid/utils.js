import get from 'lodash/get'
import map from 'lodash/map'

export const getColumnsFromDatasource = datasource => get(datasource, [0, 'columns'], [])

export const getDataFromDatasource = datasource =>
  datasource && datasource.length > 1 ? map(datasource.slice(1), item => item.row) : []
