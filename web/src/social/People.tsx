import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Alert, Button, TextField } from "@mui/material";
import { apiProblem, socialApi } from "../api/client";

export function RelationshipAction({
  target,
  action,
  children,
  after,
}: {
  target: string;
  action: string;
  children: React.ReactNode;
  after?: () => void;
}) {
  const client = useQueryClient();
  const [failure, setFailure] = useState("");
  const mutation = useMutation({
    mutationFn: () => socialApi.changeRelationship({ target, action }),
    onSuccess: async () => {
      setFailure("");
      await client.invalidateQueries({ queryKey: ["social"] });
      after?.();
    },
    onError: async (error) => setFailure((await apiProblem(error)).message),
  });
  return (
    <span>
      <Button
        size="small"
        disabled={mutation.isPending}
        onClick={() => {
          if (
            action === "block" &&
            !window.confirm(
              "Block this athlete? Both follow relationships and pending requests will be removed. Neither athlete will be able to view the other's profile or shared activities.",
            )
          )
            return;
          mutation.mutate();
        }}
      >
        {mutation.isPending ? "Updating…" : children}
      </Button>
      {failure && <Alert severity="error">{failure}</Alert>}
    </span>
  );
}
const sections = [
  { value: "search", label: "Search" },
  { value: "following", label: "Following" },
  { value: "followers", label: "Followers" },
  { value: "requests", label: "Requests" },
  { value: "pending", label: "Sent requests" },
  { value: "blocks", label: "Blocked" },
];
export function People() {
  const [kind, setKind] = useState("search"),
    [input, setInput] = useState(""),
    [term, setTerm] = useState(""),
    [page, setPage] = useState(0);
  const results = useQuery({
    queryKey: ["social", "people", kind, term, page],
    refetchInterval: kind === "requests" ? 5000 : false,
    enabled: kind !== "search" || term.length >= 2,
    queryFn: ({ signal }) =>
      kind === "search"
        ? socialApi.searchAthletes({ query: term }, { signal })
        : socialApi.listRelationships({ kind, page }, { signal }),
  });
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="eyebrow">TRAIN TOGETHER</p>
          <h1>People</h1>
        </div>
      </div>
      <div className="people-tabs" aria-label="People views">
        {sections.map((section) => (
          <Button
            key={section.value}
            aria-pressed={kind === section.value}
            variant={kind === section.value ? "contained" : "text"}
            onClick={() => {
              setKind(section.value);
              setPage(0);
            }}
          >
            {section.label}
          </Button>
        ))}
      </div>
      {kind === "search" && (
        <form
          className="people-search"
          onSubmit={(event) => {
            event.preventDefault();
            const next = input.trim();
            if (next === term && next.length >= 2) void results.refetch();
            else setTerm(next);
          }}
        >
          <TextField
            label="Find athletes"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            required
            slotProps={{ htmlInput: { minLength: 2, maxLength: 80 } }}
            helperText="Search first name, last name, username or handle. At least 2 characters."
          />
          <Button type="submit" variant="contained">
            Search
          </Button>
        </form>
      )}
      {results.isFetching && <p role="status">Finding athletes…</p>}
      {results.isError && (
        <Alert severity="error">
          Could not load athletes.{" "}
          <Button onClick={() => void results.refetch()}>Retry</Button>
        </Alert>
      )}
      {results.isSuccess && results.data.length === 0 && (
        <p>
          {kind === "requests"
            ? "No incoming follow requests."
            : kind === "pending"
              ? "No sent follow requests."
              : kind === "search"
                ? "No athletes match your search."
                : "No athletes here yet."}
        </p>
      )}
      <ul className="people-list">
        {results.data?.map((person) => (
          <li key={person.id}>
            <div>
              {kind === "blocks" ? (
                <strong>{person.displayName}</strong>
              ) : (
                <Link to={`/athletes/${person.handle}`}>
                  {person.displayName}
                </Link>
              )}
              <p>@{person.handle}</p>
            </div>
            <div className="people-actions">
              {person.id && (
                <>
                  {kind === "requests" && (
                    <>
                      <RelationshipAction target={person.id} action="accept">
                        Accept
                      </RelationshipAction>
                      <RelationshipAction target={person.id} action="remove">
                        Decline
                      </RelationshipAction>
                    </>
                  )}
                  {kind === "followers" && (
                    <RelationshipAction target={person.id} action="remove">
                      Remove follower
                    </RelationshipAction>
                  )}
                  {(kind === "following" || kind === "pending") && (
                    <RelationshipAction target={person.id} action="unfollow">
                      {kind === "pending" ? "Cancel request" : "Unfollow"}
                    </RelationshipAction>
                  )}
                  {kind === "blocks" && (
                    <RelationshipAction target={person.id} action="unblock">
                      Unblock
                    </RelationshipAction>
                  )}
                </>
              )}
            </div>
          </li>
        ))}
      </ul>
      {kind === "requests" && (
        <Button
          onClick={() => void results.refetch()}
          disabled={results.isFetching}
        >
          Refresh requests
        </Button>
      )}
      {kind !== "search" && (page > 0 || (results.data?.length ?? 0) >= 20) && (
        <div className="people-actions">
          <Button disabled={page === 0} onClick={() => setPage(page - 1)}>
            Previous
          </Button>
          <span>Page {page + 1}</span>
          <Button
            disabled={!results.data || results.data.length < 20}
            onClick={() => setPage(page + 1)}
          >
            Next
          </Button>
        </div>
      )}
    </>
  );
}
