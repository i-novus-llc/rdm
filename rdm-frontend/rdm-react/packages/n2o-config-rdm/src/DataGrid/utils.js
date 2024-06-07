import get from 'lodash/get'

const defaultData = []

export const getColumnsFromDatasource = datasource => get(datasource, [0, 'columnsConfig'])

export const getDataFromDatasource = model => {
  if (!model) return defaultData;

  return (
    model.reduce((out, item) => {
      if (item.row && Object.keys(item.row).length) {
        out.push(item.row)
      }

      return out
    }, [])
  )
}
