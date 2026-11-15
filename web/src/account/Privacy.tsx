import { useState } from "react";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from "@mui/material";
import Download from "@mui/icons-material/Download";
import DeleteOutlined from "@mui/icons-material/DeleteOutlined";
import LockOutlined from "@mui/icons-material/LockOutlined";
import { ExportToJSON } from "../api/generated";
import { apiProblem, privacyApi } from "../api/client";
import { useSession } from "./Session";

export function Privacy() {
  const session = useSession(),
    client = useQueryClient();
  const [confirm, setConfirm] = useState(false),
    [error, setError] = useState("");
  const [deleted, setDeleted] = useState(false);
  const exportData = useMutation({
    mutationFn: () => privacyApi.exportAccount(),
    onSuccess: (data) => {
      const url = URL.createObjectURL(
        new Blob([JSON.stringify(ExportToJSON(data), null, 2)], {
          type: "application/json",
        }),
      );
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = "syla-export.json";
      anchor.click();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
    },
    onError: async (error) => setError((await apiProblem(error)).message),
  });
  const deletion = useMutation({
    mutationFn: () => privacyApi.deleteAccount(),
    onSuccess: () => {
      client.clear();
      setDeleted(true);
      setConfirm(false);
    },
    onError: async (error) => {
      setError((await apiProblem(error)).message);
      setConfirm(false);
      await client.invalidateQueries({ queryKey: ["session"] });
    },
  });
  if (deleted)
    return (
      <div className="consent-card">
        <h1>Your account has been deleted.</h1>
        <p>Your Syla training data and sign-in account have been removed.</p>
        <Button href="/" variant="contained">
          Return to Syla
        </Button>
      </div>
    );
  const blocked = session?.state !== "ACTIVE";
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Privacy &amp; data</h1>
          <p>Manage your training data and account.</p>
        </div>
        <LockOutlined />
      </div>
      {error && <Alert severity="error">{error}</Alert>}
      {blocked && (
        <Alert severity="warning">
          Account deletion is in progress. Activity access is blocked. Retry
          deletion to finish any remaining identity cleanup.
        </Alert>
      )}
      <section className="form-card privacy-card">
        <h2>Activity privacy</h2>
        <p>
          Only you can see your activity records. Heart rate, energy, effort,
          and equipment details are optional. There is no public feed or
          location tracking in this release.
        </p>
      </section>
      <section className="form-card privacy-card">
        <h2>Export your data</h2>
        <p>
          Download your activities, entered metrics, account identifier, and
          recorded consent in a machine-readable JSON file.
        </p>
        <Button
          variant="outlined"
          startIcon={<Download />}
          disabled={blocked || exportData.isPending}
          onClick={() => {
            setError("");
            exportData.mutate();
          }}
        >
          {exportData.isPending ? "Preparing export…" : "Export my data"}
        </Button>
      </section>
      <section className="form-card privacy-card danger-card">
        <h2>Withdraw consent &amp; delete account</h2>
        <p>
          This stops training-data processing, deletes all your activities and
          consent, and removes your Keycloak sign-in account. If identity
          cleanup fails, access stays blocked and deletion can be retried.
        </p>
        <p>
          A hashed sign-in blocker is retained for up to 25 hours after
          successful deletion to prevent an old session from recreating your
          account. It contains no workout details or name.
        </p>
        <Button
          color="error"
          variant="outlined"
          startIcon={<DeleteOutlined />}
          onClick={() => setConfirm(true)}
        >
          {blocked ? "Retry account deletion" : "Delete my account"}
        </Button>
      </section>
      <Dialog
        open={confirm}
        onClose={() => !deletion.isPending && setConfirm(false)}
      >
        <DialogTitle>Delete your account and all activities?</DialogTitle>
        <DialogContent>
          Your workouts, training consent, and sign-in account will be removed.
          Export your data first if you want a copy. This cannot be undone.
        </DialogContent>
        <DialogActions>
          <Button
            disabled={deletion.isPending}
            onClick={() => setConfirm(false)}
          >
            Keep my account
          </Button>
          <Button
            color="error"
            variant="contained"
            disabled={deletion.isPending}
            onClick={() => deletion.mutate()}
          >
            {deletion.isPending ? "Deleting…" : "Permanently delete account"}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
