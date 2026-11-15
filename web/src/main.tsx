import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { CssBaseline, ThemeProvider, createTheme } from "@mui/material";
import { App } from "./App";
import "./styles.css";

const theme = createTheme({
  palette: {
    primary: { main: "#284d3b" },
    background: { default: "#f6f7f3", paper: "#ffffff" },
    text: { primary: "#172b22", secondary: "#59665d" },
  },
  typography: { fontFamily: "Inter, ui-sans-serif, system-ui, sans-serif" },
  shape: { borderRadius: 16 },
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
