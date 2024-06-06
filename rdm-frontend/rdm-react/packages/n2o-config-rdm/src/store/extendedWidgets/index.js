import { createSlice } from '@reduxjs/toolkit'
import set from 'lodash/set'
import { widgetSlice, initialState } from 'n2o-framework/lib/ducks/widgets/store'


const widgetsSlice = createSlice({
  name: widgetSlice.name,
  initialState,
  reducers: {
    ...widgetSlice.caseReducers,
    rdmUpdateConfigField: {
      prepare(widgetId, field, value) {
        return ({
          payload: { widgetId, field, value },
        })
      },
      reducer(state, action) {
        const { widgetId, field, value } = action.payload
        const widget = state[widgetId]

        if (widget) {
          set(widget, field, value)
        }
      },
    },
  },
})

export const {
  rdmUpdateConfigField,
} = widgetsSlice.actions

export const widgetsReducer = widgetsSlice.reducer
