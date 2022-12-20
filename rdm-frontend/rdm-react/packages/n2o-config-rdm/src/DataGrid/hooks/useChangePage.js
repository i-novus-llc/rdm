import {useCallback, useEffect, useRef} from 'react'
import isEqual from 'lodash/isEqual'
import isEmpty from 'lodash/isEmpty'

import { getDataFromDatasource } from '../utils'

export const useChangePage = ({
  activePage,
  count,
  size,
  models,
  setPage,
  fetchData,
}) => {
  const prevData = useRef([])
  const { datasource, filter = {} } = models

  const onChangePage = useCallback(page => {
    fetchData({
      page,
      ...filter,
    })
  }, [fetchData, filter])

  useEffect(() => {
    const data = getDataFromDatasource(datasource)
    const notEqualData = !isEqual(prevData.current, data)

    if (notEqualData) {
      const needSetPage = isEmpty(data) && count > 0 && activePage > 1

      if (needSetPage) {
        setPage(Math.ceil(count / size))
      }

      prevData.current = datasource
    }
  }, [datasource, activePage, count, size])

  return onChangePage
}
