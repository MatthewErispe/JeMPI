import { GridToolbarContainer, GridToolbarQuickFilter } from '@mui/x-data-grid'
import { FC } from 'react'
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs'
import { DesktopDatePicker } from '@mui/x-date-pickers/DesktopDatePicker'
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider'
import { Select, MenuItem, InputLabel, FormControl } from '@mui/material'
import dayjs, {Dayjs} from 'dayjs'
import { SelectChangeEvent } from '@mui/material/Select';
import * as React from 'react';

interface DataGridToolbarProps {
  onChangeDate: (date: Dayjs | null) => void;
  onChangeState: (state: string) => void;
  valueDate: Dayjs;
  valueState: string;
}

const DataGridToolbar: FC<DataGridToolbarProps> = ({
      onChangeDate,
      onChangeState,
      valueDate,
      valueState
       }) => {
      const handleChange = (date: Dayjs | null) => {
        onChangeDate
      }
      const handleChangeState = (event: SelectChangeEvent) => {
            onChangeState(event.target.value);
      };
  // const onClick = () => {
  //     props.sendData(user)
  // }

  return (
    <GridToolbarContainer
      sx={{ p: 2, display: 'flex', gap: 2, alignItems: 'baseLine' }}
    >

      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <DesktopDatePicker
          value={valueDate}
          format="YYYY-MM-DD"
          onChange={value => onChangeDate(value)}
          slotProps={{
            textField: {
              variant: 'standard',
              size: 'small'
            },
          }}
        />
      </LocalizationProvider>
     <FormControl sx={{ m: 1, minWidth: 120 }} size="small">
                        <InputLabel id="demo-select-small-label">State</InputLabel>
                        <Select
                          labelId="demo-select-small-label"
                          id="demo-select-small"
                          value={valueState}
                          label="State"
                          onChange={handleChangeState}

                        >
                          <MenuItem value="">
                            <em>None</em>
                          </MenuItem>
                          <MenuItem value='New'>New</MenuItem>
                          <MenuItem value='Closed'>Closed</MenuItem>
                          <MenuItem value='Pending'>Pending</MenuItem>
                        </Select>
                      </FormControl>
      <GridToolbarQuickFilter
        placeholder="Type to filter results"
        quickFilterParser={searchInput =>
          searchInput.split(',').map(value => value.trim())
        }
        quickFilterFormatter={quickFilterValues =>
          quickFilterValues ? quickFilterValues.join(', ') : ''
        }
        debounceMs={200}
      />
    </GridToolbarContainer>
  )
}

export default DataGridToolbar
