import { createTheme } from '@mui/material/styles';

const GLASS_BG = 'rgba(255, 255, 255, 0.07)';
const GLASS_BORDER = '1px solid rgba(255, 255, 255, 0.12)';
const GLASS_BLUR = 'blur(16px) saturate(180%)';

const theme = createTheme({
  palette: {
    mode: 'dark',
    primary: { main: '#818cf8', light: '#a5b4fc', dark: '#6366f1' },
    secondary: { main: '#34d399', light: '#6ee7b7', dark: '#10b981' },
    background: { default: 'transparent', paper: GLASS_BG },
    text: { primary: '#f1f5f9', secondary: '#94a3b8' },
    divider: 'rgba(255, 255, 255, 0.08)',
    error: { main: '#f87171' },
    warning: { main: '#fbbf24' },
    success: { main: '#34d399' },
    info: { main: '#38bdf8' },
  },
  shape: { borderRadius: 12 },
  typography: {
    fontFamily: '"Inter", "Roboto", "Helvetica", "Arial", sans-serif',
    h4: { fontWeight: 700 },
    h5: { fontWeight: 700 },
    h6: { fontWeight: 600 },
    subtitle1: { fontWeight: 600 },
    subtitle2: { fontWeight: 600 },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          background: GLASS_BG,
          backdropFilter: GLASS_BLUR,
          WebkitBackdropFilter: GLASS_BLUR,
          border: GLASS_BORDER,
          borderRadius: 16,
          transition: 'transform 0.2s ease, box-shadow 0.2s ease',
          '&:hover': {
            transform: 'translateY(-2px)',
            boxShadow: '0 8px 32px rgba(129, 140, 248, 0.15)',
          },
        },
      },
    },
    MuiPaper: {
      styleOverrides: {
        root: {
          background: GLASS_BG,
          backdropFilter: GLASS_BLUR,
          WebkitBackdropFilter: GLASS_BLUR,
          border: GLASS_BORDER,
          borderRadius: 12,
          backgroundImage: 'none',
        },
        elevation1: { boxShadow: '0 4px 24px rgba(0, 0, 0, 0.2)' },
        elevation3: { boxShadow: '0 8px 32px rgba(0, 0, 0, 0.3)' },
      },
    },
    MuiButton: {
      defaultProps: { variant: 'contained' },
      styleOverrides: {
        root: {
          minHeight: 44,
          borderRadius: 10,
          textTransform: 'none',
          fontWeight: 600,
          transition: 'all 0.2s ease',
        },
        contained: {
          background: 'linear-gradient(135deg, #6366f1, #818cf8)',
          boxShadow: '0 4px 15px rgba(99, 102, 241, 0.35)',
          '&:hover': {
            background: 'linear-gradient(135deg, #4f46e5, #6366f1)',
            boxShadow: '0 6px 20px rgba(99, 102, 241, 0.55)',
            transform: 'translateY(-1px)',
          },
          '&:disabled': {
            background: 'rgba(255,255,255,0.12)',
            boxShadow: 'none',
          },
        },
        outlined: {
          border: '1px solid rgba(255, 255, 255, 0.22)',
          '&:hover': {
            background: 'rgba(255, 255, 255, 0.07)',
            border: '1px solid rgba(255, 255, 255, 0.38)',
          },
        },
        text: {
          '&:hover': { background: 'rgba(255, 255, 255, 0.07)' },
        },
      },
    },
    MuiTextField: { defaultProps: { variant: 'outlined' } },
    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          background: 'rgba(255, 255, 255, 0.05)',
          borderRadius: 10,
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: 'rgba(129, 140, 248, 0.5)',
          },
          '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
            borderColor: '#818cf8',
            boxShadow: '0 0 0 3px rgba(129, 140, 248, 0.12)',
          },
        },
        notchedOutline: { borderColor: 'rgba(255, 255, 255, 0.15)' },
      },
    },
    MuiInputLabel: {
      styleOverrides: { root: { color: '#94a3b8' } },
    },
    MuiDrawer: {
      styleOverrides: {
        paper: {
          background: 'rgba(15, 12, 41, 0.92)',
          backdropFilter: 'blur(24px)',
          WebkitBackdropFilter: 'blur(24px)',
          borderRight: '1px solid rgba(255, 255, 255, 0.08)',
        },
      },
    },
    MuiAppBar: {
      styleOverrides: {
        root: {
          background: 'rgba(15, 12, 41, 0.88)',
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          boxShadow: 'none',
          borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
        },
      },
    },
    MuiChip: {
      styleOverrides: {
        root: { borderRadius: 8, fontWeight: 500, border: '1px solid' },
        colorDefault: {
          background: 'rgba(255,255,255,0.08)',
          borderColor: 'rgba(255,255,255,0.15)',
        },
        colorPrimary: {
          background: 'rgba(129, 140, 248, 0.18)',
          borderColor: 'rgba(129, 140, 248, 0.38)',
          color: '#a5b4fc',
        },
        colorSecondary: {
          background: 'rgba(52, 211, 153, 0.18)',
          borderColor: 'rgba(52, 211, 153, 0.38)',
          color: '#6ee7b7',
        },
        colorWarning: {
          background: 'rgba(251, 191, 36, 0.18)',
          borderColor: 'rgba(251, 191, 36, 0.38)',
          color: '#fcd34d',
        },
        colorSuccess: {
          background: 'rgba(52, 211, 153, 0.18)',
          borderColor: 'rgba(52, 211, 153, 0.38)',
          color: '#6ee7b7',
        },
        colorError: {
          background: 'rgba(248, 113, 113, 0.18)',
          borderColor: 'rgba(248, 113, 113, 0.38)',
          color: '#fca5a5',
        },
        colorInfo: {
          background: 'rgba(56, 189, 248, 0.18)',
          borderColor: 'rgba(56, 189, 248, 0.38)',
          color: '#7dd3fc',
        },
      },
    },
    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-root': {
            background: 'rgba(255, 255, 255, 0.04)',
            fontWeight: 600,
            color: '#94a3b8',
            fontSize: '0.72rem',
            textTransform: 'uppercase',
            letterSpacing: '0.06em',
          },
        },
      },
    },
    MuiTableRow: {
      styleOverrides: {
        root: { '&:hover': { background: 'rgba(129, 140, 248, 0.05)' } },
      },
    },
    MuiTableCell: {
      styleOverrides: {
        root: { borderBottom: '1px solid rgba(255, 255, 255, 0.05)' },
      },
    },
    MuiDivider: {
      styleOverrides: { root: { borderColor: 'rgba(255, 255, 255, 0.08)' } },
    },
    MuiAlert: {
      styleOverrides: {
        root: { backdropFilter: 'blur(8px)', borderRadius: 10, border: '1px solid' },
        standardError: {
          background: 'rgba(248, 113, 113, 0.12)',
          borderColor: 'rgba(248, 113, 113, 0.28)',
        },
        standardSuccess: {
          background: 'rgba(52, 211, 153, 0.12)',
          borderColor: 'rgba(52, 211, 153, 0.28)',
        },
        standardWarning: {
          background: 'rgba(251, 191, 36, 0.12)',
          borderColor: 'rgba(251, 191, 36, 0.28)',
        },
        standardInfo: {
          background: 'rgba(56, 189, 248, 0.12)',
          borderColor: 'rgba(56, 189, 248, 0.28)',
        },
      },
    },
    MuiDialog: {
      styleOverrides: {
        paper: {
          background: 'rgba(15, 12, 41, 0.95)',
          backdropFilter: 'blur(24px)',
          WebkitBackdropFilter: 'blur(24px)',
          border: '1px solid rgba(255, 255, 255, 0.12)',
          borderRadius: 16,
          backgroundImage: 'none',
        },
      },
    },
    MuiDialogTitle: {
      styleOverrides: { root: { fontWeight: 700 } },
    },
    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          margin: '1px 8px',
          width: 'calc(100% - 16px)',
          transition: 'all 0.2s ease',
          '&:hover': { background: 'rgba(255, 255, 255, 0.08)' },
          '&.Mui-selected': {
            background: 'rgba(129, 140, 248, 0.18)',
            '&:hover': { background: 'rgba(129, 140, 248, 0.24)' },
          },
        },
      },
    },
    MuiMenuItem: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          '&:hover': { background: 'rgba(255, 255, 255, 0.08)' },
          '&.Mui-selected': { background: 'rgba(129, 140, 248, 0.18)' },
        },
      },
    },
    MuiSkeleton: {
      styleOverrides: {
        root: {
          background: 'rgba(255, 255, 255, 0.08)',
          borderRadius: 8,
          '&::after': {
            background:
              'linear-gradient(90deg, transparent, rgba(255,255,255,0.07), transparent)',
          },
        },
      },
    },
    MuiBackdrop: {
      styleOverrides: {
        root: { backdropFilter: 'blur(4px)', background: 'rgba(0,0,0,0.5)' },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: {
          background: 'rgba(15, 12, 41, 0.95)',
          border: '1px solid rgba(255,255,255,0.12)',
          backdropFilter: 'blur(8px)',
          borderRadius: 8,
        },
      },
    },
  },
});

export default theme;
