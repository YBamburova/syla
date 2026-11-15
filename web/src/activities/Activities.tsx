import { useState } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import Add from "@mui/icons-material/Add";
import LockOutlined from "@mui/icons-material/LockOutlined";
import { Alert, Button, CircularProgress } from "@mui/material";
import { workoutsApi } from "../api/client";
import {
  activityDistance,
  activityPace,
  dateLabel,
  duration,
  sports,
  sportName,
  type Sport,
} from "./format";
import { SportIcon } from "./SportIcon";

export function Activities() {
  const [sport, setSport] = useState<Sport | undefined>();
  const [page, setPage] = useState(0);
  const activities = useQuery({
    queryKey: ["activities", sport, page],
    queryFn: ({ signal }) =>
      workoutsApi.listWorkouts({ sport, page, size: 12 }, { signal }),
    retry: false,
  });
  return (
    <>
      <div className="page-heading">
        <div>
          <h1>Activities</h1>
        </div>
        <Button
          component={Link}
          to="/activities/new"
          variant="contained"
          startIcon={<Add />}
        >
          Log activity
        </Button>
      </div>
      <div className="sport-filters" aria-label="Filter activities by sport">
        <button
          className={!sport ? "active" : ""}
          aria-pressed={!sport}
          onClick={() => {
            setSport(undefined);
            setPage(0);
          }}
        >
          All
        </button>
        {sports.map((item) => (
          <button
            className={sport === item.value ? "active" : ""}
            aria-pressed={sport === item.value}
            key={item.value}
            onClick={() => {
              setSport(item.value);
              setPage(0);
            }}
          >
            {item.label}
          </button>
        ))}
      </div>
      {activities.isPending ? (
        <div className="center-state" role="status">
          <CircularProgress size={24} />
          <p>Loading activities…</p>
        </div>
      ) : activities.isError ? (
        <Alert
          severity="error"
          action={
            <Button onClick={() => void activities.refetch()}>Retry</Button>
          }
        >
          Couldn’t load your activities. If your session expired,{" "}
          <a href="/oauth2/authorization/keycloak">sign in again</a>.
        </Alert>
      ) : activities.data.items.length === 0 ? (
        <div className="journal-empty">
          <span className="empty-illustration">
            <SportIcon sport={sport ?? "RUN"} />
          </span>
          <h2>
            {sport
              ? `No ${sportName(sport).toLowerCase()} activities yet.`
              : "Your first activity belongs here."}
          </h2>
          <p>
            Finished a session? Add the basics now.
            <br />
            You can always fill in the details later.
          </p>
          <Button
            component={Link}
            to={`/activities/new${sport ? "?sport=" + sport : ""}`}
            variant="contained"
            startIcon={<Add />}
          >
            Log{" "}
            {sport
              ? `a ${sportName(sport).toLowerCase()}`
              : "your first activity"}
          </Button>
        </div>
      ) : (
        <div className="activity-list">
          {activities.data.items.map((item) => {
            const pace = activityPace(item),
              distance = activityDistance(item);
            return (
              <Link
                className="activity-card"
                to={`/activities/${item.id}`}
                key={item.id}
              >
                <span
                  className={`sport-badge ${sports.find((s) => s.value === item.activity.sport)?.color}`}
                >
                  <SportIcon sport={item.activity.sport} />
                </span>
                <div className="activity-card-body">
                  <span className="activity-date">
                    {sportName(item.activity.sport)} ·{" "}
                    {dateLabel(item.activity.trainingDate)}
                  </span>
                  <h3>{item.activity.title}</h3>
                  <span className="activity-audience">
                    <LockOutlined fontSize="inherit" /> Only you
                  </span>
                  <div className="activity-stats">
                    {distance && (
                      <div>
                        <strong>{distance}</strong>
                        <span>Distance</span>
                      </div>
                    )}
                    <div>
                      <strong>{duration(item.activity.durationSeconds)}</strong>
                      <span>Duration</span>
                    </div>
                    {pace && (
                      <div>
                        <strong>
                          {pace.value}
                          <small>{pace.label}</small>
                        </strong>
                        <span>
                          {item.activity.sport === "RIDE"
                            ? "Avg. speed"
                            : "Avg. pace"}
                          {item.activity.metrics.movingSeconds
                            ? " · moving"
                            : ""}
                        </span>
                      </div>
                    )}
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
      {activities.data && activities.data.total > 12 && (
        <div className="pagination">
          <Button disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </Button>
          <span>
            Page {page + 1} of {Math.ceil(activities.data.total / 12)}
          </span>
          <Button
            disabled={(page + 1) * 12 >= activities.data.total}
            onClick={() => setPage(page + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </>
  );
}
