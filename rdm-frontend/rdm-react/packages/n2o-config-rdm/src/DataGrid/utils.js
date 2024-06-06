import get from 'lodash/get'
import map from 'lodash/map'

const defaultColumns = {
  cells: [],
  headers: []
}

const defaultData = []

export const getColumnsFromDatasource = datasource => get(datasource, [0, 'columnsConfig'], defaultColumns)

export const getDataFromDatasource = datasource => datasource ? map(datasource, item => item.row) : defaultData
