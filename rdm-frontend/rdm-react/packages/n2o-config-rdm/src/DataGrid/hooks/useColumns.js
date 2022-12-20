import React, { useState, useEffect, useRef } from 'react'
import factoryResolver from 'n2o-framework/lib/core/factory/factoryResolver'
import columnHOC from 'n2o-framework/lib/components/widgets/Table/withColumn'
import TableCell from 'n2o-framework/lib/components/widgets/Table/TableCell'
import map from 'lodash/map'
import omit from 'lodash/omit'
import isObject from 'lodash/isObject'
import isEqual from 'lodash/isEqual'

import { getColumnsFromDatasource } from '../utils'

const ReduxCell = columnHOC(TableCell)

export const useColumns = ({ id, datasourceModel, sorting, onSort }) => {
  const prevColumns = useRef([])
  const [columns, setColumns] = useState([])

  useEffect(() => {
    const columns = getColumnsFromDatasource(datasourceModel)

    if (!isEqual(prevColumns.current, columns)) {
      setColumns(prepareColumns(columns))

      prevColumns.current = columns
    }
  }, [datasourceModel])

  const prepareColumns = columns => {
    return map(columns, (item) => {
      let newItem = Object.assign({}, item)

      newItem = {
        ...newItem,
        src: 'TextTableHeader',
        id: newItem.key,
        dataIndex: newItem.key,
        width: newItem.width,
        filterControl: newItem.filterControl,
        filterable: newItem.filterable,
        resizable: newItem.resizable,
        sortable: newItem.sortable,
      }

      const resolvedProps = factoryResolver(omit(newItem, ['filterControl']))

      return {
        ...newItem,
        title: (
          <ReduxCell
            {...resolvedProps}
            label={item.name}
            columnId={newItem.key}
            widgetId={id}
            as='div'
            sorting={sorting && sorting[newItem.key]}
            onSort={onSort}
          />
        ),
        render: value => ({
          children: isObject(value) ? value['value'] : value,
          props: {
            valueKey: newItem.key
          }
        })
      }
    })
  }

  return columns
}
