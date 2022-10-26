import { useState, useEffect, useRef } from 'react'
import first from 'lodash/first'
import isEqual from 'lodash/isEqual'
import isEmpty from 'lodash/isEmpty'

import { getDataFromDatasource } from '../utils'

export const useData = ({ models, setResolve }) => {
  const prevData = useRef([])
  const [data, setData] = useState([])
  const { datasource } = models

    useEffect(() => {
      const data = getDataFromDatasource(datasource)

      if (!isEqual(prevData.current, data)) {
        setData(data)

        if (isEmpty(prevData.current) && !isEmpty(data)) {
          setResolve(first(data))
        }

        prevData.current = data
      }
    }, [datasource])

  return data
}
