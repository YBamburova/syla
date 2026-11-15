import { useRef, useState } from "react";
import { useForm, type FieldPath, type RegisterOptions } from "react-hook-form";
import {
  Link,
  useBeforeUnload,
  useBlocker,
  useNavigate,
  useSearchParams,
} from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  TextField,
} from "@mui/material";
import ArrowBack from "@mui/icons-material/ArrowBack";
import Check from "@mui/icons-material/Check";
import ExpandMore from "@mui/icons-material/ExpandMore";
import LockOutlined from "@mui/icons-material/LockOutlined";
import { apiProblem, workoutsApi } from "../api/client";
import type { ActivityInput, Metrics, WorkoutView } from "../api/generated";
import { calendarDate, duration, pace, sports, type Sport } from "./format";
import { SportIcon } from "./SportIcon";

type Values = {
  sport: Sport;
  title: string;
  date: string;
  startTime: string;
  timeZone: string;
  hours: string;
  minutes: string;
  seconds: string;
  venue: Metrics["venue"];
  distance: string;
  distanceUnit: NonNullable<Metrics["distanceUnit"]>;
  poolMode: "distance" | "lengths";
  poolLength: string;
  poolLengthUnit: "M" | "YD";
  lengths: string;
  stroke: NonNullable<Metrics["stroke"]> | "";
  movingHours: string;
  movingMinutes: string;
  movingSeconds: string;
  elevationGain: string;
  elevationUnit: "M" | "FT";
  averageHeartRate: string;
  maxHeartRate: string;
  calories: string;
  cadence: string;
  averagePower: string;
  gear: string;
  effort: string;
  description: string;
};
const number = (value: string) => (value === "" ? undefined : Number(value));
const text = (value: number | undefined) =>
  value == null ? "" : String(value);
const today = () => {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
};
const fieldMap: Record<string, FieldPath<Values>> = {
  trainingDate: "date",
  durationSeconds: "minutes",
  "metrics.distance": "distance",
  "metrics.movingSeconds": "movingMinutes",
  "metrics.elevationGain": "elevationGain",
  "metrics.maxHeartRate": "maxHeartRate",
  "metrics.poolLength": "poolLength",
  "metrics.poolLengthUnit": "poolLengthUnit",
  "metrics.venue": "venue",
  "metrics.averagePower": "averagePower",
  "metrics.cadence": "cadence",
  "metrics.stroke": "stroke",
};

function defaults(
  item: WorkoutView | undefined,
  requestedSport: string | null,
): Values {
  const a = item?.activity,
    m = a?.metrics;
  const sport =
    a?.sport ?? sports.find((s) => s.value === requestedSport)?.value ?? "RUN";
  const total = a?.durationSeconds ?? 0,
    moving = m?.movingSeconds ?? 0;
  return {
    sport,
    title: a?.title ?? "",
    date: a ? calendarDate(a.trainingDate) : today(),
    startTime: a?.startTime?.slice(0, 5) ?? "",
    timeZone: a?.timeZone ?? Intl.DateTimeFormat().resolvedOptions().timeZone,
    hours: total >= 3600 ? String(Math.floor(total / 3600)) : "",
    minutes: total ? String(Math.floor((total % 3600) / 60)) : "",
    seconds: total % 60 ? String(total % 60) : "",
    venue:
      m?.venue ??
      (sport === "SWIM" ? "POOL" : sport === "WORKOUT" ? "INDOOR" : "OUTDOOR"),
    distance: text(m?.distance),
    distanceUnit: m?.distanceUnit ?? (sport === "SWIM" ? "M" : "KM"),
    poolMode: m?.lengths ? "lengths" : "distance",
    poolLength: text(m?.poolLength) || "25",
    poolLengthUnit: m?.poolLengthUnit === "YD" ? "YD" : "M",
    lengths: text(m?.lengths),
    stroke: m?.stroke ?? "",
    movingHours: moving >= 3600 ? String(Math.floor(moving / 3600)) : "",
    movingMinutes: moving ? String(Math.floor((moving % 3600) / 60)) : "",
    movingSeconds: moving % 60 ? String(moving % 60) : "",
    elevationGain: text(m?.elevationGain),
    elevationUnit: m?.elevationUnit ?? "M",
    averageHeartRate: text(m?.averageHeartRate),
    maxHeartRate: text(m?.maxHeartRate),
    calories: text(m?.calories),
    cadence: text(m?.cadence),
    averagePower: text(m?.averagePower),
    gear: m?.gear ?? "",
    effort: text(a?.effort),
    description: a?.description ?? "",
  };
}

export function ActivityForm({ item: suppliedItem }: { item?: WorkoutView }) {
  // An open edit keeps the revision it was based on, even if the query cache refreshes.
  const item = useRef(suppliedItem).current;
  const [params] = useSearchParams();
  const initial = useRef(defaults(item, params.get("sport")));
  type Choices = Pick<
    Values,
    | "sport"
    | "venue"
    | "distanceUnit"
    | "poolMode"
    | "poolLengthUnit"
    | "elevationUnit"
    | "stroke"
    | "effort"
  >;
  const [choices, setChoices] = useState<Choices>(() => ({
    sport: initial.current.sport,
    venue: initial.current.venue,
    distanceUnit: initial.current.distanceUnit,
    poolMode: initial.current.poolMode,
    poolLengthUnit: initial.current.poolLengthUnit,
    elevationUnit: initial.current.elevationUnit,
    stroke: initial.current.stroke,
    effort: initial.current.effort,
  }));
  const [choicesDirty, setChoicesDirty] = useState(false);
  function setValue<K extends keyof Choices>(
    name: K,
    value: Choices[K],
    _options?: { shouldDirty?: boolean },
  ) {
    setChoices((previous) => ({ ...previous, [name]: value }));
    setChoicesDirty(true);
  }
  const {
    register,
    watch,
    handleSubmit,
    setError,
    formState: { errors, isDirty: fieldsDirty },
  } = useForm<Values>({ defaultValues: initial.current });
  function bind(name: FieldPath<Values>, options?: RegisterOptions<Values>) {
    const { ref, ...field } = register(name, options);
    return { ...field, inputRef: ref };
  }
  const v = { ...watch(), ...choices };
  const isDirty = fieldsDirty || choicesDirty;

  const navigate = useNavigate();
  const client = useQueryClient();
  const [failure, setFailure] = useState("");
  const [sessionExpired, setSessionExpired] = useState(false);
  const leaving = useRef(false),
    submission = useRef({ key: crypto.randomUUID(), payload: "" });
  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      isDirty &&
      !leaving.current &&
      currentLocation.pathname !== nextLocation.pathname,
  );
  useBeforeUnload((event) => {
    if (isDirty && !leaving.current) {
      event.preventDefault();
      event.returnValue = "";
    }
  });
  const pool = v.sport === "SWIM" && v.venue === "POOL",
    endurance = v.sport !== "WORKOUT";
  const seconds =
    Number(v.hours || 0) * 3600 +
    Number(v.minutes || 0) * 60 +
    Number(v.seconds || 0);
  const moving =
    Number(v.movingHours || 0) * 3600 +
    Number(v.movingMinutes || 0) * 60 +
    Number(v.movingSeconds || 0);
  const rawDistance =
    pool && v.poolMode === "lengths"
      ? Number(v.poolLength) * Number(v.lengths)
      : Number(v.distance);
  const unit =
    pool && v.poolMode === "lengths" ? v.poolLengthUnit : v.distanceUnit;
  const meters =
    rawDistance * { KM: 1000, MI: 1609.344, M: 1, YD: 0.9144 }[unit];
  const paceTime = moving || seconds;
  const livePace =
    meters > 0 && paceTime > 0
      ? v.sport === "RIDE"
        ? `${(((meters / paceTime) * 3.6) / (unit === "MI" ? 1.609344 : 1)).toFixed(1)} ${unit === "MI" ? "mph" : "km/h"}`
        : `${pace((paceTime / meters) * (v.sport === "SWIM" ? (unit === "YD" ? 91.44 : 100) : unit === "MI" ? 1609.344 : 1000))} /${v.sport === "SWIM" ? (unit === "YD" ? "100 yd" : "100 m") : unit === "MI" ? "mi" : "km"}`
      : null;

  const save = useMutation({
    mutationFn: async (input: ActivityInput) => {
      const payload = JSON.stringify(input);
      if (submission.current.payload && submission.current.payload !== payload)
        submission.current.key = crypto.randomUUID();
      submission.current.payload = payload;
      const options = { signal: AbortSignal.timeout(20_000) };
      return item
        ? workoutsApi.updateWorkout(
            {
              id: item.id,
              activityInput: input,
              ifMatch: `"${item.revision}"`,
            },
            options,
          )
        : workoutsApi.createWorkout(
            { activityInput: input, idempotencyKey: submission.current.key },
            options,
          );
    },
    onSuccess: async (saved) => {
      leaving.current = true;
      await client.invalidateQueries({ queryKey: ["activities"] });
      client.setQueryData(["activity", saved.id], saved);
      navigate(`/activities/${saved.id}`, {
        replace: true,
        state: { saved: true },
      });
    },
    onError: async (error) => {
      const problem = await apiProblem(error);
      setFailure(problem.message);
      setSessionExpired(problem.status === 401);
      for (const [key, message] of Object.entries(problem.fields)) {
        const field =
          fieldMap[key] ??
          (key.startsWith("metrics.") ? key.substring(8) : key);
        if (field in v) setError(field as FieldPath<Values>, { message });
      }
    },
  });

  function changeSport(sport: Sport) {
    setValue("sport", sport);
    setValue(
      "venue",
      sport === "SWIM" ? "POOL" : sport === "WORKOUT" ? "INDOOR" : "OUTDOOR",
    );
    setValue("distanceUnit", sport === "SWIM" ? "M" : "KM");
  }

  function submit(values: Values) {
    setFailure("");
    setSessionExpired(false);
    if (!seconds || seconds > 604800) {
      setError(
        "minutes",
        { message: "Enter a duration between 1 second and 7 days." },
        { shouldFocus: true },
      );
      return;
    }
    if (moving > seconds) {
      setError(
        "movingMinutes",
        { message: "Moving time cannot exceed total duration." },
        { shouldFocus: true },
      );
      return;
    }
    const poolLengths = pool && values.poolMode === "lengths";
    const metrics: Metrics = {
      venue: values.venue,
      distance: endurance && !poolLengths ? number(values.distance) : undefined,
      distanceUnit:
        endurance && !poolLengths && values.distance
          ? values.distanceUnit
          : undefined,
      movingSeconds: endurance && moving > 0 ? moving : undefined,
      elevationGain:
        (values.sport === "RUN" || values.sport === "RIDE") &&
        values.venue === "OUTDOOR"
          ? number(values.elevationGain)
          : undefined,
      elevationUnit:
        (values.sport === "RUN" || values.sport === "RIDE") &&
        values.venue === "OUTDOOR" &&
        values.elevationGain !== ""
          ? values.elevationUnit
          : undefined,
      averageHeartRate: number(values.averageHeartRate),
      maxHeartRate: number(values.maxHeartRate),
      calories: number(values.calories),
      cadence:
        values.sport === "RUN" || values.sport === "RIDE"
          ? number(values.cadence)
          : undefined,
      averagePower:
        values.sport === "RIDE" ? number(values.averagePower) : undefined,
      poolLength: poolLengths ? number(values.poolLength) : undefined,
      poolLengthUnit: poolLengths ? values.poolLengthUnit : undefined,
      lengths: poolLengths ? number(values.lengths) : undefined,
      stroke:
        values.sport === "SWIM" && values.stroke ? values.stroke : undefined,
      gear: values.gear.trim() || undefined,
    };
    save.mutate({
      sport: values.sport,
      title: values.title.trim() || undefined,
      trainingDate: new Date(values.date + "T12:00:00"),
      startTime: values.startTime ? values.startTime + ":00" : undefined,
      timeZone: values.timeZone,
      durationSeconds: seconds,
      metrics,
      effort: number(values.effort),
      description: values.description.trim() || undefined,
    });
  }

  function numeric(
    name: FieldPath<Values>,
    label: string,
    max = 100000,
    required = false,
    decimal = false,
  ) {
    return (
      <TextField
        label={label}
        type="number"
        fullWidth
        error={!!errors[name]}
        helperText={errors[name]?.message}
        slotProps={{
          htmlInput: {
            min: required ? 0.001 : 0,
            max,
            step: decimal ? "any" : 1,
            inputMode: decimal ? "decimal" : "numeric",
          },
        }}
        {...bind(name, {
          required: required ? "Enter a value." : false,
          min: {
            value: required ? 0.001 : 0,
            message: "Enter a positive value.",
          },
          max: { value: max, message: `Must be ${max} or less.` },
          validate: (value) =>
            value === "" ||
            (Number.isFinite(Number(value)) &&
              (decimal || Number.isInteger(Number(value)))) ||
            "Enter a valid number.",
        })}
      />
    );
  }

  return (
    <>
      <Link to={item ? `/activities/${item.id}` : "/"} className="back-link">
        <ArrowBack fontSize="small" />
        {item ? "Back to activity" : "Back to activities"}
      </Link>
      <div className="page-heading">
        <div>
          <p className="eyebrow">MAKE YOUR EFFORT COUNT</p>
          <h1>{item ? "Edit activity" : "Log an activity"}</h1>
          <p>Start with the basics. Add as much detail as you like.</p>
        </div>
        <span className="privacy-note">
          <LockOutlined fontSize="inherit" /> Private · only you
        </span>
      </div>
      <form
        onSubmit={handleSubmit((values) => submit({ ...values, ...choices }))}
        noValidate
        className="activity-form"
      >
        <div className="form-main">
          <section className="form-card">
            <div className="card-section-title">
              <span>01</span>
              <h2>What did you do?</h2>
            </div>
            <fieldset className="sport-picker" aria-label="Activity type">
              {sports.map((sport) => (
                <label
                  key={sport.value}
                  className={v.sport === sport.value ? "selected" : ""}
                >
                  <input
                    type="radio"
                    name="activitySport"
                    value={sport.value}
                    checked={v.sport === sport.value}
                    onChange={() => changeSport(sport.value)}
                  />
                  <SportIcon sport={sport.value} />
                  <span>{sport.label}</span>
                  {v.sport === sport.value && <Check className="sport-check" />}
                </label>
              ))}
            </fieldset>
            <div className="form-row">
              <TextField
                label="Activity title (optional)"
                placeholder={
                  v.sport === "RUN"
                    ? "e.g. Easy morning run"
                    : v.sport === "SWIM"
                      ? "e.g. Lunchtime pool session"
                      : v.sport === "RIDE"
                        ? "e.g. Weekend ride"
                        : "e.g. Evening gym session"
                }
                fullWidth
                {...bind("title", {
                  maxLength: {
                    value: 120,
                    message: "Use 120 characters or fewer.",
                  },
                })}
                error={!!errors.title}
                helperText={errors.title?.message}
              />
              <TextField
                select
                label="Setting"
                value={v.venue}
                onChange={(e) =>
                  setValue("venue", e.target.value as Metrics["venue"])
                }
                fullWidth
              >
                {v.sport === "SWIM"
                  ? [
                      <MenuItem value="POOL" key="pool">
                        Pool swim
                      </MenuItem>,
                      <MenuItem value="OPEN_WATER" key="open">
                        Open water
                      </MenuItem>,
                    ]
                  : [
                      <MenuItem value="OUTDOOR" key="out">
                        Outdoors
                      </MenuItem>,
                      <MenuItem value="INDOOR" key="in">
                        {v.sport === "RUN" ? "Treadmill / indoors" : "Indoors"}
                      </MenuItem>,
                    ]}
              </TextField>
            </div>
          </section>
          <section className="form-card">
            <div className="card-section-title">
              <span>02</span>
              <h2>The essentials</h2>
            </div>
            <div className="form-row">
              <TextField
                label="Date"
                type="date"
                fullWidth
                slotProps={{
                  inputLabel: { shrink: true },
                  htmlInput: { max: today(), min: "1900-01-01" },
                }}
                {...bind("date", {
                  required: "Choose a date.",
                  validate: (value) =>
                    value <= today() || "Choose today or a past date.",
                })}
                error={!!errors.date}
                helperText={errors.date?.message}
              />
              <TextField
                label="Start time (optional)"
                type="time"
                fullWidth
                slotProps={{ inputLabel: { shrink: true } }}
                {...bind("startTime")}
                error={!!errors.startTime}
                helperText={errors.startTime?.message}
              />
            </div>
            <TextField
              label="Time zone"
              fullWidth
              {...bind("timeZone", {
                required: "Enter an IANA time zone.",
              })}
              error={!!errors.timeZone}
              helperText={
                errors.timeZone?.message ??
                "Your activity keeps this local date and time."
              }
            />
            <fieldset className="duration-fields">
              <legend>Total duration</legend>
              <div className="form-row three">
                {numeric("hours", "Hours", 168)}
                {numeric("minutes", "Minutes", 59)}
                {numeric("seconds", "Seconds", 59)}
              </div>
              <p className="field-note">
                Include pauses and rests. Add moving time under More details if
                you know it.
              </p>
            </fieldset>
            {endurance && (
              <>
                {pool && (
                  <div className="input-mode">
                    <span>How would you like to enter distance?</span>
                    <div>
                      <button
                        type="button"
                        aria-pressed={v.poolMode === "distance"}
                        onClick={() =>
                          setValue("poolMode", "distance", {
                            shouldDirty: true,
                          })
                        }
                      >
                        Total distance
                      </button>
                      <button
                        type="button"
                        aria-pressed={v.poolMode === "lengths"}
                        onClick={() =>
                          setValue("poolMode", "lengths", { shouldDirty: true })
                        }
                      >
                        Pool lengths
                      </button>
                    </div>
                  </div>
                )}
                {pool && v.poolMode === "lengths" ? (
                  <>
                    <div className="form-row three">
                      {numeric("poolLength", "Pool length", 200, true, true)}
                      <TextField
                        label="Pool unit"
                        select
                        value={v.poolLengthUnit}
                        onChange={(e) =>
                          setValue(
                            "poolLengthUnit",
                            e.target.value as "M" | "YD",
                            { shouldDirty: true },
                          )
                        }
                      >
                        <MenuItem value="M">Meters</MenuItem>
                        <MenuItem value="YD">Yards</MenuItem>
                      </TextField>
                      {numeric("lengths", "Lengths swum", 100000, true)}
                    </div>
                    <p className="field-note">
                      One length is one trip across the pool. Distance = pool
                      length × lengths swum.
                    </p>
                  </>
                ) : (
                  <div className="form-row distance-row">
                    {numeric(
                      "distance",
                      "Distance (optional)",
                      1000000,
                      false,
                      true,
                    )}
                    <TextField
                      label="Distance unit"
                      select
                      value={v.distanceUnit}
                      onChange={(e) =>
                        setValue(
                          "distanceUnit",
                          e.target.value as NonNullable<
                            Metrics["distanceUnit"]
                          >,
                          { shouldDirty: true },
                        )
                      }
                    >
                      {(v.sport === "SWIM"
                        ? [
                            ["M", "Meters"],
                            ["YD", "Yards"],
                            ["KM", "Kilometers"],
                            ["MI", "Miles"],
                          ]
                        : [
                            ["KM", "Kilometers"],
                            ["MI", "Miles"],
                            ["M", "Meters"],
                          ]
                      ).map(([value, label]) => (
                        <MenuItem key={value} value={value}>
                          {label}
                        </MenuItem>
                      ))}
                    </TextField>
                  </div>
                )}
              </>
            )}
          </section>
          <Accordion className="details-accordion" defaultExpanded={!!item}>
            <AccordionSummary expandIcon={<ExpandMore />}>
              <div>
                <h2>
                  More details <span>Optional</span>
                </h2>
                <p>Effort, health metrics, equipment, and notes</p>
              </div>
            </AccordionSummary>
            <AccordionDetails>
              {endurance && (
                <fieldset className="duration-fields">
                  <legend>Moving time (optional)</legend>
                  <div className="form-row three">
                    {numeric("movingHours", "Moving hours", 168)}
                    {numeric("movingMinutes", "Moving minutes", 59)}
                    {numeric("movingSeconds", "Moving seconds", 59)}
                  </div>
                  <p className="field-note">
                    Excludes rests. Used for pace and speed when supplied.
                  </p>
                </fieldset>
              )}
              <div className="form-row">
                <TextField
                  label="Perceived effort (optional)"
                  select
                  value={v.effort}
                  onChange={(e) =>
                    setValue("effort", e.target.value, { shouldDirty: true })
                  }
                  fullWidth
                >
                  <MenuItem value="">Not recorded</MenuItem>
                  {Array.from({ length: 10 }, (_, i) => (
                    <MenuItem key={i + 1} value={String(i + 1)}>
                      {i + 1} ·{" "}
                      {i < 2
                        ? "Very easy"
                        : i < 4
                          ? "Easy"
                          : i < 6
                            ? "Moderate"
                            : i < 8
                              ? "Hard"
                              : "Very hard"}
                    </MenuItem>
                  ))}
                </TextField>
                {numeric("calories", "Energy (kcal, optional)")}
              </div>
              <div className="form-row">
                {numeric(
                  "averageHeartRate",
                  "Avg. heart rate (bpm, optional)",
                  300,
                )}
                {numeric(
                  "maxHeartRate",
                  "Max. heart rate (bpm, optional)",
                  300,
                )}
              </div>
              {(v.sport === "RUN" || v.sport === "RIDE") &&
                v.venue === "OUTDOOR" && (
                  <div className="form-row">
                    {numeric(
                      "elevationGain",
                      "Elevation gain (optional)",
                      100000,
                      false,
                      true,
                    )}
                    <TextField
                      label="Elevation unit"
                      select
                      value={v.elevationUnit}
                      onChange={(e) =>
                        setValue(
                          "elevationUnit",
                          e.target.value as "M" | "FT",
                          { shouldDirty: true },
                        )
                      }
                    >
                      <MenuItem value="M">Meters</MenuItem>
                      <MenuItem value="FT">Feet</MenuItem>
                    </TextField>
                  </div>
                )}
              {(v.sport === "RUN" || v.sport === "RIDE") && (
                <div className="form-row">
                  {numeric(
                    "cadence",
                    v.sport === "RUN"
                      ? "Avg. cadence (steps/min, optional)"
                      : "Avg. cadence (rpm, optional)",
                    300,
                  )}
                  {v.sport === "RIDE" &&
                    numeric(
                      "averagePower",
                      "Avg. power (watts, optional)",
                      3000,
                    )}
                </div>
              )}
              {v.sport === "SWIM" && (
                <TextField
                  label="Stroke (optional)"
                  select
                  value={v.stroke}
                  onChange={(e) =>
                    setValue("stroke", e.target.value as Values["stroke"], {
                      shouldDirty: true,
                    })
                  }
                  fullWidth
                >
                  <MenuItem value="">Not recorded</MenuItem>
                  {[
                    "FREESTYLE",
                    "BREASTSTROKE",
                    "BACKSTROKE",
                    "BUTTERFLY",
                    "MIXED",
                    "OTHER",
                  ].map((value) => (
                    <MenuItem key={value} value={value}>
                      {value[0] + value.slice(1).toLowerCase()}
                    </MenuItem>
                  ))}
                </TextField>
              )}
              <TextField
                label="Equipment (optional)"
                placeholder={
                  v.sport === "RIDE"
                    ? "Bike name"
                    : v.sport === "RUN"
                      ? "Shoe name"
                      : "Equipment used"
                }
                fullWidth
                {...bind("gear", { maxLength: 120 })}
              />
              <TextField
                label="Notes (optional)"
                placeholder="How did it feel? Anything you’d like to remember?"
                multiline
                minRows={3}
                fullWidth
                {...bind("description", {
                  maxLength: {
                    value: 5000,
                    message: "Use 5,000 characters or fewer.",
                  },
                })}
                error={!!errors.description}
                helperText={errors.description?.message}
              />
            </AccordionDetails>
          </Accordion>
          {failure && (
            <Alert severity="error" role="alert">
              {failure}
              {sessionExpired && (
                <p>
                  <a
                    href="/oauth2/authorization/keycloak"
                    target="_blank"
                    rel="noopener noreferrer"
                  >
                    Sign in in a new tab
                  </a>
                  , then retry here. Keep this page open to retain your entry.
                </p>
              )}
            </Alert>
          )}
          <div className="form-actions">
            <Button
              component={Link}
              to={item ? `/activities/${item.id}` : "/"}
              disabled={save.isPending}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={save.isPending}
              startIcon={<Check />}
            >
              {save.isPending
                ? "Saving…"
                : item
                  ? "Save changes"
                  : "Save activity"}
            </Button>
          </div>
        </div>
        <aside className="activity-preview">
          <span
            className={`sport-badge ${sports.find((s) => s.value === v.sport)?.color}`}
          >
            <SportIcon sport={v.sport} />
          </span>
          <p className="eyebrow">YOUR ACTIVITY AT A GLANCE</p>
          <h2>{v.title || sports.find((s) => s.value === v.sport)?.noun}</h2>
          <p>{v.date}</p>
          <div className="preview-stat">
            <span>Total duration</span>
            <strong>{seconds > 0 ? duration(seconds) : "Not entered"}</strong>
          </div>
          {endurance && (
            <div className="preview-stat">
              <span>Distance</span>
              <strong>
                {rawDistance > 0
                  ? `${rawDistance.toLocaleString()} ${unit.toLowerCase()}`
                  : "Not entered"}
              </strong>
            </div>
          )}
          {livePace && (
            <div className="preview-stat">
              <span>
                {v.sport === "RIDE" ? "Average speed" : "Average pace"}
                {moving ? " · moving" : ""}
              </span>
              <strong>{livePace}</strong>
            </div>
          )}
          <div className="preview-privacy">
            <LockOutlined fontSize="small" />
            <span>
              Only you can see this activity.
              <br />
              You can edit or delete it later.
            </span>
          </div>
        </aside>
      </form>
      <Dialog open={blocker.state === "blocked"}>
        <DialogTitle>Discard this activity entry?</DialogTitle>
        <DialogContent>Your unsaved changes will be lost.</DialogContent>
        <DialogActions>
          <Button
            onClick={() => blocker.state === "blocked" && blocker.reset()}
          >
            Keep editing
          </Button>
          <Button
            color="error"
            onClick={() => blocker.state === "blocked" && blocker.proceed()}
          >
            Discard changes
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
