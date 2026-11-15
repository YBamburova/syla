import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { CssBaseline, ThemeProvider, createTheme } from "@mui/material";
import { App } from "./App";
import "./styles.css";

const theme = createTheme({
  palette: {
    primary: { main: "#E32636", dark: "#C51D2B", contrastText: "#ffffff" },
    background: { default: "#ffffff", paper: "#ffffff" },
    text: { primary: "#202420", secondary: "#626862" },
  },
  typography: { fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif" },
  shape: { borderRadius: 8 },
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: {
          textTransform: "none",
          fontWeight: 600,
          minHeight: 44,
          paddingInline: 20,
        },
      },
    },
    MuiTextField: {
      defaultProps: {
        variant: "outlined",
        slotProps: { inputLabel: { shrink: true } },
      },
    },
    MuiAccordion: { defaultProps: { disableGutters: true } },
  },
});

const queryClient = new QueryClient();
const root = document.getElementById("root");
if (!root) throw new Error("Application root is missing");

ReactDOM.createRoot(root).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <App />
      </QueryClientProvider>
    </ThemeProvider>
  </React.StrictMode>,
);
