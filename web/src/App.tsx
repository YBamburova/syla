import { useState } from "react";
import {
  createBrowserRouter,
  Link,
  Navigate,
  NavLink,
  Outlet,
  RouterProvider,
} from "react-router-dom";
import { Alert, Button } from "@mui/material";
import Add from "@mui/icons-material/Add";
import CalendarMonthOutlined from "@mui/icons-material/CalendarMonthOutlined";
import LockOutlined from "@mui/icons-material/LockOutlined";
import Logout from "@mui/icons-material/Logout";
import { SessionGate, signOut, useSession } from "./account/Session";
import { Privacy } from "./account/Privacy";

function Shell() {
  const session = useSession();
  const [error, setError] = useState("");
  const blocked = session?.state !== "ACTIVE";
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <Link className="wordmark" to="/" aria-label="Syla home">
          syla<span aria-hidden="true">↗</span>
        </Link>
        <p className="sidebar-label">YOUR SPACE</p>
        <nav aria-label="Main navigation">
          <NavLink to="/" end>
            <CalendarMonthOutlined />
            Activities
          </NavLink>
          <NavLink to="/privacy">
            <LockOutlined />
            Privacy &amp; data
          </NavLink>
        </nav>
        <div className="sidebar-bottom">
          <span className="sidebar-monogram">
            {session?.displayName?.slice(0, 1).toUpperCase() || "S"}
          </span>
          <div>
            <strong>{session?.displayName}</strong>
            <span>Your personal training space</span>
          </div>
        </div>
      </aside>
      <div className="workspace">
        <header className="workspace-header">
          <Link to="/" className="mobile-wordmark">
            syla↗
          </Link>
          <span className="header-note">A LITTLE STRONGER, EVERY DAY.</span>
          <div>
            <span className="private-label">
              <LockOutlined fontSize="inherit" />
              Private journal
            </span>
            <Button
              size="small"
              startIcon={<Logout />}
              onClick={() =>
                void signOut().catch(() =>
                  setError("Could not sign out. Refresh and try again."),
                )
              }
            >
              Sign out
            </Button>
          </div>
        </header>
        <nav className="mobile-nav" aria-label="Mobile navigation">
          <NavLink to="/" end>
            <CalendarMonthOutlined />
            Activities
          </NavLink>
          <NavLink to="/privacy">
            <LockOutlined />
            Privacy &amp; data
          </NavLink>
        </nav>
        <main id="main" className="workspace-main">
          {error && <Alert severity="error">{error}</Alert>}
          {blocked ? <Privacy /> : <Outlet />}
        </main>
        <footer className="workspace-footer">
          <span>syla · strength, shared.</span>
          <details>
            <summary>Developer tools</summary>
            <a href="/swagger-ui.html">Swagger API explorer</a>
            <a href="/v3/api-docs">OpenAPI JSON</a>
          </details>
        </footer>
      </div>
    </div>
  );
}
function Root() {
  return (
    <SessionGate>
      <Shell />
    </SessionGate>
  );
}
export const appRoutes = [
  {
    hydrateFallbackElement: (
      <div className="center-state" role="status">
        Opening your training space…
      </div>
    ),
    element: <Root />,
    children: [
      {
        index: true,
        lazy: async () => ({
          Component: (await import("./activities/Activities")).Activities,
        }),
      },
      {
        path: "activities/new",
        lazy: async () => ({
          Component: (await import("./activities/ActivityForm")).ActivityForm,
        }),
      },
      {
        path: "activities/:id",
        lazy: async () => ({
          Component: (await import("./activities/ActivityDetail"))
            .ActivityDetail,
        }),
      },
      {
        path: "activities/:id/edit",
        lazy: async () => {
          const { ActivityDetail } =
            await import("./activities/ActivityDetail");
          return { Component: () => <ActivityDetail edit /> };
        },
      },
      { path: "privacy", element: <Privacy /> },
      { path: "*", element: <Navigate to="/" replace /> },
    ],
  },
];
let browserRouter: ReturnType<typeof createBrowserRouter> | undefined;
export function App() {
  browserRouter ??= createBrowserRouter(appRoutes);
  return <RouterProvider router={browserRouter} />;
}
