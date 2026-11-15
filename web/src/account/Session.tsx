import { createContext, useContext, useState, type ReactNode } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Checkbox,
  FormControlLabel,
  CircularProgress,
} from "@mui/material";
import type { AccountView } from "../api/generated";
import { accountApi, apiProblem } from "../api/client";

const SessionContext = createContext<AccountView | null>(null);
export const useSession = () => useContext(SessionContext);

function LinkBrand() {
  return <span className="wordmark">Syla</span>;
}

export function SessionGate({ children }: { children: ReactNode }) {
  const client = useQueryClient();
  const session = useQuery({
    queryKey: ["session"],
    queryFn: ({ signal }) =>
      accountApi.getSession({
        signal: AbortSignal.any([signal, AbortSignal.timeout(10000)]),
      }),
    retry: false,
  });
  const [agreed, setAgreed] = useState(false);
  const [error, setError] = useState("");
  const consent = useMutation({
    mutationFn: () => accountApi.acceptConsent(),
    onSuccess: () => client.invalidateQueries({ queryKey: ["session"] }),
    onError: async (error) => setError((await apiProblem(error)).message),
  });
  if (session.isPending)
    return (
      <div className="center-state" role="status">
        <CircularProgress size={28} />
        <p>Opening your training space…</p>
      </div>
    );
  if (session.isError)
    return (
      <div className="center-state">
        <Alert severity="error">We couldn’t reach your training space.</Alert>
        <Button onClick={() => void session.refetch()}>Try again</Button>
      </div>
    );
  if (!session.data.authenticated)
    return (
      <div className="welcome-screen">
        <LinkBrand />
        <h1>Your training, in one place.</h1>
        <p>
          Log runs, swims, rides, and workouts.
          <br />
          Keep a private record of your training.
        </p>
        <Button
          variant="contained"
          size="large"
          href="/oauth2/authorization/keycloak"
        >
          Sign in or create an account
        </Button>
        <span className="welcome-note">
          Your activities are private. Always yours to edit, export, or delete.
        </span>
        {new URLSearchParams(window.location.search).get("login") ===
          "failed" && (
          <Alert severity="error">
            Sign-in did not finish. Please try again.
          </Alert>
        )}
      </div>
    );
  if (session.data.state !== "ACTIVE")
    return (
      <SessionContext.Provider value={session.data}>
        {children}
      </SessionContext.Provider>
    );
  if (!session.data.consented)
    return (
      <section className="consent-card">
        <p className="eyebrow">Training data consent</p>
        <h1>Your training. Your choice.</h1>
        <p>
          Syla stores the activities you enter so you can keep a private
          training history. Optional details like heart rate, calories, and
          effort are saved only when you add them.
        </p>
        <p>
          Your registration name and username identify you to other signed-in
          athletes. Your athlete profile is ready automatically, and follow
          requests require approval by default. Workouts stay private until you
          share them.
        </p>
        <p>
          You can export your records or withdraw consent and delete your
          account in Privacy &amp; data. We don’t send your activity data to AI
          or wearable providers in this feature.
        </p>
        <FormControlLabel
          control={
            <Checkbox
              checked={agreed}
              onChange={(event) => setAgreed(event.target.checked)}
            />
          }
          label="I consent to storing my training details, including any optional health metrics I enter, for personal logging and history."
        />
        {error && <Alert severity="error">{error}</Alert>}
        <Button
          variant="contained"
          disabled={!agreed || consent.isPending}
          onClick={() => consent.mutate()}
        >
          Continue to my activities
        </Button>
      </section>
    );
  return (
    <SessionContext.Provider value={session.data}>
      {children}
    </SessionContext.Provider>
  );
}

export async function signOut() {
  const csrf = await accountApi.getCsrf();
  if (!csrf.parameterName || !csrf.token)
    throw new Error("Refresh this page to sign out.");
  const form = document.createElement("form");
  form.method = "POST";
  form.action = "/logout";
  const input = document.createElement("input");
  input.type = "hidden";
  input.name = csrf.parameterName;
  input.value = csrf.token;
  form.append(input);
  document.body.append(form);
  form.submit();
}
