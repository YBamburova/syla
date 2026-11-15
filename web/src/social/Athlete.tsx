import { useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Alert, Button } from "@mui/material";
import { sharedActivitiesApi, socialApi } from "../api/client";
import { dateLabel, duration, sportName, pace } from "../activities/format";
import type { SharedActivityView } from "../api/generated";
import { RelationshipAction } from "./People";

function Summary({ activity }: { activity: SharedActivityView }) {
  return (
    <>
      <h1>{activity.title}</h1>
      <p>
        {activity.sport && sportName(activity.sport)}
        {activity.trainingDate && ` · ${dateLabel(activity.trainingDate)}`}
      </p>
      <dl className="detail-grid">
        <div>
          <dt>Duration</dt>
          <dd>{duration(activity.durationSeconds ?? 0)}</dd>
        </div>
        {activity.distanceMeters != null && (
          <div>
            <dt>Distance</dt>
            <dd>
              {(activity.distanceMeters / 1000).toLocaleString(undefined, {
                maximumFractionDigits: 3,
              })}{" "}
              km
            </dd>
          </div>
        )}
        {activity.sport === "RIDE" && activity.averageSpeedKph != null && (
          <div>
            <dt>Average speed</dt>
            <dd>{activity.averageSpeedKph.toFixed(1)} km/h</dd>
          </div>
        )}
        {activity.sport === "RUN" && activity.paceSecondsPerKm != null && (
          <div>
            <dt>Average pace</dt>
            <dd>{pace(activity.paceSecondsPerKm)} /km</dd>
          </div>
        )}
        {activity.sport === "SWIM" &&
          activity.swimPaceSecondsPer100m != null && (
            <div>
              <dt>Average pace</dt>
              <dd>{pace(activity.swimPaceSecondsPer100m)} /100 m</dd>
            </div>
          )}
      </dl>
      <p className="privacy-note">
        Shared activity summary. Notes, health metrics and exact start times are
        private.
      </p>
    </>
  );
}
export function SharedActivity() {
  const { id = "" } = useParams();
  const activity = useQuery({
    queryKey: ["social", "shared", id],
    queryFn: ({ signal }) =>
      sharedActivitiesApi.getSharedActivity({ id }, { signal }),
    retry: false,
  });
  if (activity.isPending) return <p role="status">Opening shared activity…</p>;
  if (activity.isError)
    return (
      <Alert severity="info">
        Activity unavailable. It may be private, deleted, or no longer shared
        with you. <Link to="/athletes">Back to people</Link>
      </Alert>
    );
  return (
    <>
      <Link className="back-link" to="/athletes">
        Back to people
      </Link>
      <Summary activity={activity.data} />
    </>
  );
}
export function Athlete() {
  const { handle = "" } = useParams();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const athlete = useQuery({
    queryKey: ["social", "athlete", handle],
    queryFn: ({ signal }) => socialApi.getAthlete({ handle }, { signal }),
    retry: false,
  });
  const profile = athlete.data?.profile;
  const permitted =
    athlete.data?.relationship === "SELF" ||
    athlete.data?.relationship === "ACCEPTED";
  const activities = useQuery({
    queryKey: ["social", "shared-list", profile?.id, page],
    enabled: !!profile?.id && permitted,
    queryFn: ({ signal }) =>
      sharedActivitiesApi.listSharedActivities(
        { owner: profile!.id!, page },
        { signal },
      ),
    retry: false,
  });
  if (athlete.isPending) return <p role="status">Opening athlete…</p>;
  if (athlete.isError || !profile)
    return (
      <Alert severity="info">
        Athlete unavailable. <Link to="/athletes">Back to people</Link>
      </Alert>
    );
  const relationship = athlete.data.relationship;
  return (
    <>
      <Link className="back-link" to="/athletes">
        Back to people
      </Link>
      <div className="page-heading">
        <div>
          <p className="eyebrow">@{profile.handle}</p>
          <h1>{profile.displayName}</h1>
          <p>
            {profile.privateProfile
              ? "Follow requests require approval"
              : "Open to followers"}
          </p>
        </div>
        <div className="people-actions">
          {relationship === "SELF" ? (
            <Button component={Link} to="/profile">
              Edit profile
            </Button>
          ) : (
            profile.id && (
              <>
                <RelationshipAction
                  target={profile.id}
                  action={relationship === "NONE" ? "follow" : "unfollow"}
                >
                  {relationship === "NONE"
                    ? profile.privateProfile
                      ? "Request to follow"
                      : "Follow"
                    : relationship === "PENDING"
                      ? "Cancel request"
                      : "Unfollow"}
                </RelationshipAction>
                <RelationshipAction
                  target={profile.id}
                  action="block"
                  after={() => navigate("/athletes")}
                >
                  Block
                </RelationshipAction>
              </>
            )
          )}
        </div>
      </div>
      <p>
        {athlete.data.followers ?? 0} followers · {athlete.data.following ?? 0}{" "}
        following
      </p>
      {profile.bio && <p className="profile-bio">{profile.bio}</p>}
      <h2>Shared activities</h2>
      {!permitted && (
        <p>
          {relationship === "PENDING"
            ? "Your follow request is waiting for approval."
            : "Follow this athlete to see activities they choose to share."}
        </p>
      )}
      {permitted && activities.isPending && (
        <p role="status">Loading shared activities…</p>
      )}
      {permitted && activities.isError && (
        <Alert severity="info">
          Shared activities are unavailable.{" "}
          <Button onClick={() => void athlete.refetch()}>Refresh access</Button>
        </Alert>
      )}
      {permitted && activities.isSuccess && (
        <>
          {activities.data.items?.length === 0 && (
            <p>No shared activities yet. Private activities stay hidden.</p>
          )}
          <ul className="people-list">
            {activities.data.items?.map((item) => (
              <li key={item.id}>
                <div>
                  <Link to={`/shared-activities/${item.id}`}>{item.title}</Link>
                  <p>
                    {item.sport && sportName(item.sport)} ·{" "}
                    {item.trainingDate && dateLabel(item.trainingDate)} ·{" "}
                    {duration(item.durationSeconds ?? 0)}
                    {item.distanceMeters != null &&
                      ` · ${item.distanceMeters / 1000} km`}
                  </p>
                </div>
              </li>
            ))}
          </ul>
          {(activities.data.total ?? 0) > 20 && (
            <div className="people-actions">
              <Button disabled={page === 0} onClick={() => setPage(page - 1)}>
                Previous
              </Button>
              <span>Page {page + 1}</span>
              <Button
                disabled={(page + 1) * 20 >= (activities.data.total ?? 0)}
                onClick={() => setPage(page + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}
    </>
  );
}
