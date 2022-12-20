import { useState, useEffect, useRef } from 'react'
import first from 'lodash/first'
import isEqual from 'lodash/isEqual'
import isEmpty from 'lodash/isEmpty'

import { getDataFromDatasource } from '../utils'

export const useData = ({ datasourceModel, setResolve }) => {
  const prevData = useRef([])
  const [data, setData] = useState([])

  useEffect(() => {
    const data = getDataFromDatasource(datasourceModel)

    if (!isEqual(prevData.current, data)) {
      setData(data)

      if (isEmpty(prevData.current) && !isEmpty(data)) {
        setResolve(first(data))
      }

      prevData.current = data
    }
  }, [datasourceModel, setResolve])

  return data
}
