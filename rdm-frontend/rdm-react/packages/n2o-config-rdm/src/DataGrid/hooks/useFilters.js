import { useCallback, useState } from 'react'

export const useFilters = ({ filterModel, setFilter, fetchData }) => {
  const [filters, setFilterState] = useState({})

  const onFilter = useCallback((newFilter) => {
    const filter = {
      ...filterModel,
      [`filter.${newFilter.id}`]: newFilter.value,
    }

    setFilterState(({
      ...filters,
      [newFilter.id]: newFilter.value,
    }))
    setFilter(filter)
    fetchData({
      ...filter,
      page: 1
    })
  }, [setFilter, filterModel, fetchData])

  return { filters, onFilter }
}
