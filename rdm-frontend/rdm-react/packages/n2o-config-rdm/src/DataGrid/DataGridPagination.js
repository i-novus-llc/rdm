import React from 'react'
import Pagination from 'n2o-framework/lib/components/snippets/Pagination/Pagination'

import { useChangePage } from './hooks/useChangePage'

export function DataGridPagination(props) {
    const {
      activePage,
      count,
      size,
      models,
      setPage,
      fetchData,
    } = props

    const onChangePage = useChangePage({
      activePage,
      count,
      size,
      models,
      setPage,
      fetchData,
    })

    return (
        <Pagination
          {...props}
          onSelect={onChangePage}
          count={count > 0 ? count - 1 : count}
          stepIncrement={10}
        />
    )
}

