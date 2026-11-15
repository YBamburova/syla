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
import Logout from "@mui/icons-material/Logout";
import { SessionGate, signOut, useSession } from "./account/Session";
import { Privacy } from "./account/Privacy";

function Shell() {
  const session = useSession();
  const [error, setError] = useState("");
  const blocked = session?.state !== "ACTIVE";
  return (
    <div className="app-shell">
      <div className="workspace">
        <header className="workspace-header">
          <Link className="wordmark" to="/" aria-label="Syla home">
            Syla
          </Link>
          <nav className="main-nav" aria-label="Main navigation">
            <NavLink to="/" end>
              Activities
            </NavLink>
            <NavLink to="/privacy">Privacy &amp; data</NavLink>
          </nav>
          <div className="account-controls">
            <span className="account-name">{session?.displayName}</span>
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
        <main id="main" className="workspace-main">
          {error && <Alert severity="error">{error}</Alert>}
          {blocked ? <Privacy /> : <Outlet />}
        </main>
        <footer className="workspace-footer">
          <span>Syla</span>
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
