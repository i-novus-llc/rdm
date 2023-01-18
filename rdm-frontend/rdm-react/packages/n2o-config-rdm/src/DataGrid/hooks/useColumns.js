import React, { useState, useEffect, useRef } from 'react'
import factoryResolver from 'n2o-framework/lib/core/factory/factoryResolver'
import columnHOC from 'n2o-framework/lib/components/widgets/Table/withColumn'
import TableCell from 'n2o-framework/lib/components/widgets/Table/TableCell'
import Factory from 'n2o-framework/lib/core/factory/Factory'
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
    const prepareColumns = columns => {
      return map(columns, (item) => {
        let newItem = Object.assign({}, item)
        const { src, ...otherParamsFilterControl } = newItem.filterControl

        const filterControl = {
          ...otherParamsFilterControl,
          component: (props) => {
            return (
              <Factory src={src} {...props} />
            )
          }
        }

        newItem = {
          ...newItem,
          filterControl,
          src: 'TextTableHeader',
          id: newItem.key,
          dataIndex: newItem.key,
          width: newItem.width,
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

    if (!isEqual(prevColumns.current, columns)) {
      setColumns(prepareColumns(columns))

      prevColumns.current = columns
    }
  }, [datasourceModel, id, sorting, onSort])

  return columns
}
