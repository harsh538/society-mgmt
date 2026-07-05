import {
  Box,
  Card,
  CardActionArea,
  CardContent,
  LinearProgress,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';

/**
 * Responsive table:
 *   - desktop / tablet: classic MUI <Table>
 *   - mobile (< sm = 600px): stacked card list (each row = one Card)
 *
 * Props
 *   columns:     [{ field, headerName, width?, renderCell?(row) }]
 *   rows:        any[]
 *   loading:     boolean — shows LinearProgress above the table/list
 *   onRowClick:  optional(row) => void
 *   emptyText:   optional message when rows.length === 0 and !loading
 */
export default function DataTable({
  columns = [],
  rows = [],
  loading = false,
  onRowClick,
  emptyText = 'No records to display',
}) {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  const renderCell = (col, row) =>
    col.renderCell ? col.renderCell(row) : (row?.[col.field] ?? '');

  if (isMobile) {
    return (
      <Box sx={{ position: 'relative' }}>
        {loading && (
          <LinearProgress sx={{ position: 'absolute', top: 0, left: 0, right: 0 }} />
        )}
        <Stack spacing={1.5} sx={{ pt: loading ? 1 : 0 }}>
          {!loading && rows.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
              {emptyText}
            </Typography>
          )}
          {rows.map((row, idx) => {
            const inner = (
              <CardContent>
                {columns.map((col) => (
                  <Box
                    key={col.field}
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      gap: 2,
                      py: 0.5,
                    }}
                  >
                    <Typography variant="caption" color="text.secondary">
                      {col.headerName}
                    </Typography>
                    <Box sx={{ textAlign: 'right' }}>{renderCell(col, row)}</Box>
                  </Box>
                ))}
              </CardContent>
            );
            return (
              <Card key={row?.id ?? idx} variant="outlined">
                {onRowClick ? (
                  <CardActionArea onClick={() => onRowClick(row)}>
                    {inner}
                  </CardActionArea>
                ) : (
                  inner
                )}
              </Card>
            );
          })}
        </Stack>
      </Box>
    );
  }

  return (
    <Box sx={{ position: 'relative' }}>
      {loading && (
        <LinearProgress sx={{ position: 'absolute', top: 0, left: 0, right: 0, zIndex: 1 }} />
      )}
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              {columns.map((col) => (
                <TableCell key={col.field} style={{ width: col.width }}>
                  <Typography variant="subtitle2">{col.headerName}</Typography>
                </TableCell>
              ))}
            </TableRow>
          </TableHead>
          <TableBody>
            {!loading && rows.length === 0 && (
              <TableRow>
                <TableCell colSpan={columns.length}>
                  <Typography variant="body2" color="text.secondary" sx={{ py: 2 }}>
                    {emptyText}
                  </Typography>
                </TableCell>
              </TableRow>
            )}
            {rows.map((row, idx) => (
              <TableRow
                key={row?.id ?? idx}
                hover={!!onRowClick}
                onClick={onRowClick ? () => onRowClick(row) : undefined}
                sx={{ cursor: onRowClick ? 'pointer' : 'default' }}
              >
                {columns.map((col) => (
                  <TableCell key={col.field}>{renderCell(col, row)}</TableCell>
                ))}
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}
