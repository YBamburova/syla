import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { createMemoryRouter, RouterProvider } from "react-router-dom";
import { beforeEach, expect, it, vi } from "vitest";
import { appRoutes } from "../App";
import {
  accountApi,
  profilesApi,
  socialApi,
  sharedActivitiesApi,
} from "../api/client";

const profile = {
  id: "owner",
  handle: "alex",
  displayName: "Alex Athlete",
  bio: "Training together",
  privateProfile: true,
};
function open(path: string) {
  const client = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  render(
    <QueryClientProvider client={client}>
      <RouterProvider
        router={createMemoryRouter(appRoutes, { initialEntries: [path] })}
      />
    </QueryClientProvider>,
  );
}
beforeEach(() => {
  vi.restoreAllMocks();
  vi.spyOn(accountApi, "getSession").mockResolvedValue({
    authenticated: true,
    athleteId: "viewer",
    displayName: "Viewer",
    state: "ACTIVE",
    consented: true,
  });
  vi.spyOn(profilesApi, "getOwnProfile").mockResolvedValue({ profile });
});
it("prefills the registration profile and retains edits after a network failure", async () => {
  const user = userEvent.setup();
  const save = vi
    .spyOn(profilesApi, "saveProfile")
    .mockRejectedValueOnce(new Error("offline"))
    .mockResolvedValue({
      ...profile,
      handle: "alex_new",
      displayName: "Alex Updated",
    });
  open("/profile");
  const handle = await screen.findByRole("textbox", { name: "Handle" });
  expect(handle).toHaveValue("alex");
  await user.clear(handle);
  await user.type(handle, "alex_new");
  await user.clear(screen.getByRole("textbox", { name: "Display name" }));
  await user.type(
    screen.getByRole("textbox", { name: "Display name" }),
    "Alex Updated",
  );
  expect(
    screen.getByRole("checkbox", { name: "Approve follow requests" }),
  ).toBeChecked();
  await user.click(screen.getByRole("button", { name: "Save profile" }));
  await screen.findByText(/Could not reach Syla/);
  expect(screen.getByRole("textbox", { name: "Handle" })).toHaveValue(
    "alex_new",
  );
  await user.click(screen.getByRole("button", { name: "Save profile" }));
  await screen.findByText("Profile saved.");
  expect(save).toHaveBeenLastCalledWith({
    profileInput: {
      handle: "alex_new",
      displayName: "Alex Updated",
      bio: "Training together",
      privateProfile: true,
    },
  });
});
it("shows pending approval without requesting or rendering shared activities", async () => {
  vi.spyOn(socialApi, "getAthlete").mockResolvedValue({
    profile,
    relationship: "PENDING",
    followers: 0,
    following: 0,
  });
  const summaries = vi.spyOn(sharedActivitiesApi, "listSharedActivities");
  open("/athletes/alex");
  await screen.findByText("Your follow request is waiting for approval.");
  expect(screen.getByRole("button", { name: "Cancel request" })).toBeVisible();
  expect(summaries).not.toHaveBeenCalled();
});
it("accepts an incoming request and refreshes the list", async () => {
  const user = userEvent.setup();
  vi.spyOn(profilesApi, "getOwnProfile").mockResolvedValue({ profile });
  const list = vi
    .spyOn(socialApi, "listRelationships")
    .mockResolvedValue([profile]);
  const action = vi.spyOn(socialApi, "changeRelationship").mockResolvedValue();
  open("/athletes");
  await user.click(await screen.findByRole("button", { name: /^Requests$/ }));
  await user.click(await screen.findByRole("button", { name: /^Accept$/ }));
  await waitFor(() =>
    expect(action).toHaveBeenCalledWith({ target: "owner", action: "accept" }),
  );
  await waitFor(() => expect(list.mock.calls.length).toBeGreaterThan(1));
});
it("hides inaccessible shared details and offers a way back", async () => {
  vi.spyOn(sharedActivitiesApi, "getSharedActivity").mockRejectedValue(
    new Error("revoked"),
  );
  open("/shared-activities/hidden");
  expect(await screen.findByText(/Activity unavailable/)).toBeVisible();
  expect(screen.getByRole("link", { name: "Back to people" })).toHaveAttribute(
    "href",
    "/athletes",
  );
});

it("repeats a search for the same name instead of leaving stale empty results", async () => {
  const user = userEvent.setup();
  const search = vi
    .spyOn(socialApi, "searchAthletes")
    .mockResolvedValueOnce([])
    .mockResolvedValue([profile]);
  open("/athletes");
  await user.type(
    await screen.findByRole("textbox", { name: "Find athletes" }),
    "Alex",
  );
  const submit = screen.getAllByRole("button", { name: /^Search$/ }).at(-1)!;
  await user.click(submit);
  await screen.findByText("No athletes match your search.");
  await user.click(submit);
  expect(
    await screen.findByRole("link", { name: "Alex Athlete" }),
  ).toBeVisible();
  expect(search).toHaveBeenCalledTimes(2);
});
