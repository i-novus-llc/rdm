import React from 'react'
import Pagination from 'n2o-framework/lib/components/snippets/Pagination/Pagination'

import { useChangePage } from './hooks/useChangePage'

export function DataGridPagination(props) {
    const { count } = props
    const { onChangePage } = useChangePage(props)

    return (
        <Pagination
          {...props}
          onSelect={onChangePage}
          count={count > 0 ? count - 1 : count}
          stepIncrement={10}
        />
    )
}

