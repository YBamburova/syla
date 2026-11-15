import { act, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { appRoutes } from "./App";
import { accountApi, workoutsApi } from "./api/client";
import { ResponseError, type WorkoutView } from "./api/generated";

const activity: WorkoutView = {
  id: "11111111-1111-4111-8111-111111111111",
  revision: 0,
  activity: {
    sport: "RUN",
    title: "Easy run",
    trainingDate: new Date("2026-01-15T00:00:00Z"),
    timeZone: "UTC",
    durationSeconds: 1800,
    metrics: { venue: "OUTDOOR", distance: 5, distanceUnit: "KM" },
  },
  createdAt: new Date(),
  distanceMeters: 5000,
  paceSecondsPerKm: 360,
  averageSpeedKph: 10,
  visibility: "PRIVATE",
};
function renderApp(path = "/") {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  const router = createMemoryRouter(appRoutes, { initialEntries: [path] });
  const view = render(
    <QueryClientProvider client={client}>
      <RouterProvider router={router} />
    </QueryClientProvider>,
  );
  return { ...view, client };
}
beforeEach(() => {
  vi.restoreAllMocks();
  vi.spyOn(accountApi, "getSession").mockResolvedValue({
    authenticated: true,
    athleteId: "athlete",
    displayName: "Alex",
    state: "ACTIVE",
    consented: true,
  });
  vi.spyOn(workoutsApi, "listWorkouts").mockResolvedValue({
    items: [],
    total: 0,
    page: 0,
    size: 12,
  });
  vi.spyOn(workoutsApi, "getWorkout").mockResolvedValue(activity);
});

describe("activity logging", () => {
  it("shows a real empty journal with an actionable first entry", async () => {
    renderApp();
    expect(
      await screen.findByRole("heading", {
        name: "Your first activity belongs here.",
      }),
    ).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: "Log your first activity" }),
    ).toHaveAttribute("href", "/activities/new");
  });
  it("reveals pool-length entry and keeps optional metrics out of the essential form", async () => {
    const user = userEvent.setup();
    renderApp("/activities/new");
    await screen.findByRole("heading", { name: "Log an activity" });
    await user.click(screen.getByRole("radio", { name: "Swimming" }));
    await user.click(screen.getByRole("button", { name: "Pool lengths" }));
    expect(screen.getByRole("spinbutton", { name: "Pool length" })).toHaveValue(
      25,
    );
    await user.type(
      screen.getByRole("spinbutton", { name: "Lengths swum" }),
      "40",
    );
    await user.type(screen.getByRole("spinbutton", { name: "Minutes" }), "30");
    expect(screen.getByText("1,000 m")).toBeInTheDocument();
    expect(screen.getByText("3:00 /100 m")).toBeInTheDocument();
    expect(screen.getByText("More details")).toBeInTheDocument();
  });
  it("keeps entered values and reuses the idempotency key when retrying an uncertain save", async () => {
    const create = vi
      .spyOn(workoutsApi, "createWorkout")
      .mockRejectedValueOnce(new Error("connection lost"))
      .mockResolvedValue(activity);
    const user = userEvent.setup();
    renderApp("/activities/new");
    await screen.findByRole("heading", { name: "Log an activity" });
    await user.type(
      screen.getByRole("textbox", { name: "Activity title (optional)" }),
      "Easy run",
    );
    await user.type(screen.getByRole("spinbutton", { name: "Minutes" }), "30");
    await user.type(
      screen.getByRole("spinbutton", { name: "Distance (optional)" }),
      "5",
    );
    await user.click(screen.getByRole("button", { name: "Save activity" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Your entered values are kept",
    );
    expect(
      screen.getByRole("textbox", { name: "Activity title (optional)" }),
    ).toHaveValue("Easy run");
    await user.click(screen.getByRole("button", { name: "Save activity" }));
    await waitFor(() => expect(create).toHaveBeenCalledTimes(2));
    expect(create.mock.calls[0][0].idempotencyKey).toBe(
      create.mock.calls[1][0].idempotencyKey,
    );
    expect(
      await screen.findByText("Activity saved. Your effort is in the journal."),
    ).toBeInTheDocument();
  });
  it("guards against discarding an unsaved activity", async () => {
    const user = userEvent.setup();
    renderApp("/activities/new");
    await screen.findByRole("heading", { name: "Log an activity" });
    await user.type(
      screen.getByRole("textbox", { name: "Activity title (optional)" }),
      "Keep this",
    );
    await user.click(screen.getByRole("link", { name: "Cancel" }));
    const dialog = await screen.findByRole("dialog");
    await user.click(
      within(dialog).getByRole("button", { name: "Keep editing" }),
    );
    expect(
      await screen.findByRole("textbox", { name: "Activity title (optional)" }),
    ).toHaveValue("Keep this");
  });
  it("keeps an open edit and its original revision when the server record refreshes", async () => {
    const user = userEvent.setup();
    const update = vi
      .spyOn(workoutsApi, "updateWorkout")
      .mockRejectedValue(
        new ResponseError(
          new Response(
            JSON.stringify({
              detail: "Activity changed elsewhere",
              errors: {},
            }),
            { status: 409 },
          ),
        ),
      );
    const { client } = renderApp(`/activities/${activity.id}/edit`);
    const title = await screen.findByRole("textbox", {
      name: "Activity title (optional)",
    });
    await user.clear(title);
    await user.type(title, "My unsaved correction");
    act(() =>
      client.setQueryData(["activity", activity.id], {
        ...activity,
        revision: 1,
        activity: { ...activity.activity, title: "Changed in another tab" },
      }),
    );
    expect(
      screen.getByRole("textbox", { name: "Activity title (optional)" }),
    ).toHaveValue("My unsaved correction");
    await user.click(screen.getByRole("button", { name: "Save changes" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Activity changed elsewhere",
    );
    expect(update.mock.calls[0][0].ifMatch).toBe('"0"');
    expect(
      screen.getByRole("textbox", { name: "Activity title (optional)" }),
    ).toHaveValue("My unsaved correction");
  });
});
