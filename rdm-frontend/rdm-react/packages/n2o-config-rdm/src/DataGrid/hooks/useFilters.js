import { useState } from 'react'

export const useFilters = ({ models, setFilter, fetchData }) => {
  const [filters, setFilters] = useState({})
  const { filter } = models

  const onFilter = newFilter => {
    const filterModel = {
      ...filter,
      [`filter.${newFilter.id}`]: newFilter.value,
    }

    setFilters(({
      ...filters,
      [newFilter.id]: newFilter.value,
    }))
    setFilter(filterModel)
    fetchData({
      ...filterModel,
      page: 1
    })
  }

  return { filters, onFilter }
}
