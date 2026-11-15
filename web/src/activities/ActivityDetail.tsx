import { useState } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Button,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from "@mui/material";
import ArrowBack from "@mui/icons-material/ArrowBack";
import EditOutlined from "@mui/icons-material/EditOutlined";
import DeleteOutlined from "@mui/icons-material/DeleteOutlined";
import LockOutlined from "@mui/icons-material/LockOutlined";
import { apiProblem, workoutsApi } from "../api/client";
import {
  activityDistance,
  activityPace,
  dateLabel,
  duration,
  sportName,
  sports,
} from "./format";
import { SportIcon } from "./SportIcon";
import { ActivityForm } from "./ActivityForm";

export function ActivityDetail({ edit = false }: { edit?: boolean }) {
  const { id = "" } = useParams();
  const navigate = useNavigate();
  const client = useQueryClient();
  const location = useLocation();
  const [confirm, setConfirm] = useState(false),
    [failure, setFailure] = useState("");
  const activity = useQuery({
    queryKey: ["activity", id],
    queryFn: ({ signal }) => workoutsApi.getWorkout({ id }, { signal }),
    retry: false,
  });
  const remove = useMutation({
    mutationFn: () =>
      workoutsApi.deleteWorkout({
        id,
        ifMatch: `"${activity.data?.revision}"`,
      }),
    onSuccess: async () => {
      client.removeQueries({ queryKey: ["activity", id] });
      await client.invalidateQueries({ queryKey: ["activities"] });
      navigate("/", { replace: true });
    },
    onError: async (error) => {
      setFailure((await apiProblem(error)).message);
      setConfirm(false);
    },
  });
  const sharing = useMutation({
    mutationFn: () =>
      workoutsApi.shareWorkout({
        id,
        ifMatch: `"${activity.data?.revision}"`,
        sharingInput: {
          visibility:
            activity.data?.visibility === "FOLLOWERS" ? "PRIVATE" : "FOLLOWERS",
        },
      }),
    onSuccess: async (item) => {
      setFailure("");
      client.setQueryData(["activity", id], item);
      await Promise.all([
        client.invalidateQueries({ queryKey: ["activities"] }),
        client.invalidateQueries({ queryKey: ["social"] }),
      ]);
    },
    onError: async (error) => setFailure((await apiProblem(error)).message),
  });
  if (activity.isPending)
    return (
      <div className="center-state" role="status">
        <CircularProgress size={24} />
        <p>Opening activity…</p>
      </div>
    );
  if (activity.isError)
    return (
      <Alert severity="error">
        Activity unavailable. It may have been deleted, belong to another
        athlete, or your session may have expired.{" "}
        <Link to="/">Back to activities</Link>
      </Alert>
    );
  const item = activity.data,
    a = item.activity,
    m = a.metrics,
    pace = activityPace(item),
    distance = activityDistance(item);
  if (edit) return <ActivityForm key={id} item={item} />;
  const details: [string, string | undefined][] = [
    ["Setting", m.venue.replace("_", " ").toLowerCase()],
    ["Local date", dateLabel(a.trainingDate)],
    ["Local start time", a.startTime?.slice(0, 5)],
    ["Time zone", a.timeZone],
    ["Moving time", m.movingSeconds ? duration(m.movingSeconds) : undefined],
    ["Perceived effort", a.effort ? `${a.effort} / 10` : undefined],
    [
      "Elevation gain",
      m.elevationGain != null
        ? `${m.elevationGain} ${m.elevationUnit?.toLowerCase()}`
        : undefined,
    ],
    [
      "Average heart rate",
      m.averageHeartRate ? `${m.averageHeartRate} bpm` : undefined,
    ],
    [
      "Maximum heart rate",
      m.maxHeartRate ? `${m.maxHeartRate} bpm` : undefined,
    ],
    ["Energy", m.calories ? `${m.calories} kcal` : undefined],
    [
      "Average cadence",
      m.cadence
        ? `${m.cadence} ${a.sport === "RUN" ? "steps/min" : "rpm"}`
        : undefined,
    ],
    ["Average power", m.averagePower ? `${m.averagePower} W` : undefined],
    [
      "Pool length",
      m.poolLength
        ? `${m.poolLength} ${m.poolLengthUnit?.toLowerCase()}`
        : undefined,
    ],
    ["Lengths swum", m.lengths ? String(m.lengths) : undefined],
    ["Stroke", m.stroke?.toLowerCase()],
    ["Equipment", m.gear],
  ];
  return (
    <>
      <Link to="/" className="back-link">
        <ArrowBack fontSize="small" />
        Back to activities
      </Link>
      {location.state?.saved && (
        <Alert severity="success" className="saved-alert">
          Activity saved.
        </Alert>
      )}
      <div className="page-heading">
        <div>
          <p className="eyebrow">
            {sportName(a.sport).toUpperCase()} · {dateLabel(a.trainingDate)}
          </p>
          <h1>{a.title}</h1>
          <span className="privacy-note">
            <LockOutlined fontSize="inherit" />
            {item.visibility === "FOLLOWERS"
              ? "Approved followers · summary only"
              : "Only you"}{" "}
            · manually logged
          </span>
        </div>
        <Button
          component={Link}
          to={`/activities/${id}/edit`}
          variant="outlined"
          startIcon={<EditOutlined />}
        >
          Edit activity
        </Button>
      </div>
      <div className="detail-summary">
        <span
          className={`sport-badge ${sports.find((s) => s.value === a.sport)?.color}`}
        >
          <SportIcon sport={a.sport} />
        </span>
        {distance && (
          <div>
            <span>Distance</span>
            <strong>{distance}</strong>
          </div>
        )}
        <div>
          <span>Total duration</span>
          <strong>{duration(a.durationSeconds)}</strong>
        </div>
        {pace && (
          <div>
            <span>
              {a.sport === "RIDE" ? "Average speed" : "Average pace"}
              {m.movingSeconds ? " · moving" : ""}
            </span>
            <strong>
              {pace.value}
              <small>{pace.label}</small>
            </strong>
          </div>
        )}
      </div>
      <section className="form-card detail-card">
        <h2>Activity details</h2>
        <dl className="detail-grid">
          {details
            .filter(([, value]) => value !== undefined)
            .map(([label, value]) => (
              <div key={label}>
                <dt>{label}</dt>
                <dd>{value}</dd>
              </div>
            ))}
        </dl>
        {a.description && (
          <div className="activity-notes">
            <h3>Notes</h3>
            <p>{a.description}</p>
          </div>
        )}
      </section>
      <section className="form-card">
        <h2>Sharing</h2>
        <p>
          {item.visibility === "FOLLOWERS"
            ? "Your approved followers can see the title, sport, date, duration, distance and pace. Notes, health metrics and exact start time stay private."
            : "This activity is private. Sharing exposes only a summary to approved followers."}
        </p>
        <p>
          <Link to="/profile">Manage your athlete profile</Link> and follow
          approval settings.
        </p>
        <Button
          variant="outlined"
          disabled={sharing.isPending}
          onClick={() => {
            if (
              item.visibility !== "FOLLOWERS" &&
              !window.confirm(
                "Share this activity's title, sport, date, duration, distance and pace with approved followers? Notes, health metrics and exact start time stay private.",
              )
            )
              return;
            sharing.mutate();
          }}
        >
          {sharing.isPending
            ? "Updating…"
            : item.visibility === "FOLLOWERS"
              ? "Make private"
              : "Share with followers"}
        </Button>
      </section>
      {failure && <Alert severity="error">{failure}</Alert>}
      <Button
        color="error"
        startIcon={<DeleteOutlined />}
        onClick={() => setConfirm(true)}
      >
        Delete activity
      </Button>
      <Dialog
        open={confirm}
        onClose={() => !remove.isPending && setConfirm(false)}
      >
        <DialogTitle>Delete this activity?</DialogTitle>
        <DialogContent>
          This removes “{a.title}” from your history and export. This cannot be
          undone.
        </DialogContent>
        <DialogActions>
          <Button disabled={remove.isPending} onClick={() => setConfirm(false)}>
            Keep activity
          </Button>
          <Button
            color="error"
            variant="contained"
            disabled={remove.isPending}
            onClick={() => remove.mutate()}
          >
            {remove.isPending ? "Deleting…" : "Delete activity"}
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
