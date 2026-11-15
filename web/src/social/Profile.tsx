import { useEffect, useState } from "react";
import { Link, useBlocker } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import {
  Alert,
  Button,
  Checkbox,
  FormControlLabel,
  TextField,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from "@mui/material";
import { useForm, type RegisterOptions } from "react-hook-form";
import { apiProblem, profilesApi } from "../api/client";
import type { ProfileInput, ProfileView } from "../api/generated";

export function Profile() {
  const query = useQuery({
    queryKey: ["profile"],
    queryFn: ({ signal }) => profilesApi.getOwnProfile({ signal }),
  });
  if (query.isPending) return <p role="status">Opening your profile…</p>;
  if (query.isError)
    return (
      <Alert severity="error">
        Could not load your profile.{" "}
        <Button onClick={() => void query.refetch()}>Retry</Button>
      </Alert>
    );
  return <ProfileEditor profile={query.data.profile} />;
}
function ProfileEditor({ profile }: { profile?: ProfileView }) {
  const client = useQueryClient();
  const [failure, setFailure] = useState("");
  const [saved, setSaved] = useState(false);
  const {
    register,
    handleSubmit,
    reset,
    setError,
    watch,
    setValue,
    formState: { isDirty, errors },
  } = useForm<ProfileInput>({
    defaultValues: {
      handle: profile?.handle ?? "",
      displayName: profile?.displayName ?? "",
      bio: profile?.bio ?? "",
      privateProfile: profile?.privateProfile ?? true,
    },
  });
  const bind = <T extends "handle" | "displayName" | "bio">(
    name: T,
    options: RegisterOptions<ProfileInput, T>,
  ) => {
    const { ref, ...field } = register(name, options);
    return { ...field, inputRef: ref };
  };
  const blocker = useBlocker(isDirty);
  useEffect(() => {
    if (!isDirty) return;
    const guard = (event: BeforeUnloadEvent) => event.preventDefault();
    window.addEventListener("beforeunload", guard);
    return () => window.removeEventListener("beforeunload", guard);
  }, [isDirty]);
  const save = useMutation({
    mutationFn: (profileInput: ProfileInput) =>
      profilesApi.saveProfile({ profileInput }),
    onSuccess: async (result) => {
      reset({
        handle: result.handle ?? "",
        displayName: result.displayName ?? "",
        bio: result.bio ?? "",
        privateProfile: result.privateProfile ?? true,
      });
      setSaved(true);
      setFailure("");
      await Promise.all([
        client.invalidateQueries({ queryKey: ["profile"] }),
        client.invalidateQueries({ queryKey: ["social"] }),
      ]);
    },
    onError: async (error) => {
      const problem = await apiProblem(error);
      setFailure(problem.message);
      if (problem.fields.handle)
        setError("handle", { message: problem.fields.handle });
    },
  });
  return (
    <div className="social-page">
      <div className="page-heading">
        <div>
          <p className="eyebrow">ATHLETE PROFILE</p>
          <h1>My profile</h1>
        </div>
        {profile?.handle && (
          <Button component={Link} to={`/athletes/${profile.handle}`}>
            View profile
          </Button>
        )}
      </div>
      <p>
        Your profile starts with your registration name and username. Other
        signed-in athletes can find you by first name, last name, username or
        handle. Customize your profile below; workouts stay private until you
        share them.
      </p>
      {saved && <Alert severity="success">Profile saved.</Alert>}
      {failure && <Alert severity="error">{failure}</Alert>}
      <form
        className="profile-form"
        onSubmit={handleSubmit((values) => save.mutate(values))}
      >
        <TextField
          label="Handle"
          required
          {...bind("handle", {
            required: true,
            pattern: {
              value: /^[a-z][a-z0-9_]{2,29}$/,
              message:
                "Use 3–30 lowercase letters, numbers or underscores; start with a letter.",
            },
          })}
          error={!!errors.handle}
          helperText={
            errors.handle?.message ??
            "3–30 lowercase letters, numbers or underscores. Starts with a letter."
          }
        />
        <TextField
          label="Display name"
          required
          {...bind("displayName", {
            required: true,
            maxLength: 80,
            validate: (value) => !!value.trim(),
          })}
          error={!!errors.displayName}
          helperText="Up to 80 characters"
        />
        <TextField
          label="Bio (optional)"
          multiline
          minRows={3}
          {...bind("bio", { maxLength: 500 })}
          error={!!errors.bio}
          helperText="Up to 500 characters. Avoid private health information."
        />
        <FormControlLabel
          control={
            <Checkbox
              checked={watch("privateProfile")}
              onChange={(_, checked) =>
                setValue("privateProfile", checked, { shouldDirty: true })
              }
            />
          }
          label="Approve follow requests"
        />
        <p className="privacy-note">
          When approval is off, athletes can follow immediately. Existing
          approved followers stay approved when you change this setting. Shared
          summaries include title, sport, date, duration and distance/pace;
          notes and health metrics stay private.
        </p>
        <Button type="submit" variant="contained" disabled={save.isPending}>
          {save.isPending
            ? "Saving…"
            : profile
              ? "Save profile"
              : "Create profile"}
        </Button>
      </form>
      {blocker.state === "blocked" && (
        <Dialog open onClose={() => blocker.reset()}>
          <DialogTitle>Discard profile changes?</DialogTitle>
          <DialogContent>Your unsaved changes will be lost.</DialogContent>
          <DialogActions>
            <Button onClick={() => blocker.reset()}>Keep editing</Button>
            <Button onClick={() => blocker.proceed()}>Discard changes</Button>
          </DialogActions>
        </Dialog>
      )}
    </div>
  );
}
