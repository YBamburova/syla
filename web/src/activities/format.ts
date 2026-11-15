import type { ActivityInput, WorkoutView } from "../api/generated";

export type Sport = ActivityInput["sport"];
export const sports: {
  value: Sport;
  label: string;
  noun: string;
  color: string;
}[] = [
  { value: "RUN", label: "Running", noun: "Run", color: "run" },
  { value: "SWIM", label: "Swimming", noun: "Swim", color: "swim" },
  { value: "RIDE", label: "Cycling", noun: "Ride", color: "ride" },
  { value: "WORKOUT", label: "Workout", noun: "Workout", color: "workout" },
];
export const sportName = (sport: Sport) =>
  sports.find((item) => item.value === sport)?.noun ?? "Activity";
export function duration(seconds: number): string {
  const h = Math.floor(seconds / 3600),
    m = Math.floor((seconds % 3600) / 60),
    s = seconds % 60;
  return (
    [h ? `${h}h` : "", m ? `${m}m` : "", s ? `${s}s` : ""]
      .filter(Boolean)
      .join(" ") || "0m"
  );
}
export function pace(seconds: number): string {
  const rounded = Math.round(seconds);
  return `${Math.floor(rounded / 60)}:${String(rounded % 60).padStart(2, "0")}`;
}
export function calendarDate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}
export function dateLabel(date: Date | string): string {
  return new Intl.DateTimeFormat(undefined, {
    month: "short",
    day: "numeric",
    year: "numeric",
  }).format(
    new Date(
      (typeof date === "string" ? date.slice(0, 10) : calendarDate(date)) +
        "T12:00:00",
    ),
  );
}
export function activityDistance(item: WorkoutView): string | null {
  const metrics = item.activity.metrics;
  if (metrics.distance != null)
    return `${metrics.distance.toLocaleString()} ${metrics.distanceUnit?.toLowerCase()}`;
  if (metrics.poolLength && metrics.lengths)
    return `${(metrics.poolLength * metrics.lengths).toLocaleString()} ${metrics.poolLengthUnit?.toLowerCase()}`;
  return null;
}
export function activityPace(
  item: WorkoutView,
): { value: string; label: string } | null {
  const metric = item.activity.metrics;
  if (item.activity.sport === "SWIM" && item.swimPaceSecondsPer100m != null) {
    const yards =
      metric.distanceUnit === "YD" || metric.poolLengthUnit === "YD";
    return {
      value: pace(item.swimPaceSecondsPer100m * (yards ? 0.9144 : 1)),
      label: `/${yards ? "100 yd" : "100 m"}`,
    };
  }
  if (item.activity.sport === "RUN" && item.paceSecondsPerKm != null) {
    const miles = metric.distanceUnit === "MI";
    return {
      value: pace(item.paceSecondsPerKm * (miles ? 1.609344 : 1)),
      label: `/${miles ? "mi" : "km"}`,
    };
  }
  if (item.activity.sport === "RIDE" && item.averageSpeedKph != null) {
    const miles = metric.distanceUnit === "MI";
    return {
      value: (item.averageSpeedKph / (miles ? 1.609344 : 1)).toFixed(1),
      label: miles ? "mph" : "km/h",
    };
  }
  return null;
}
