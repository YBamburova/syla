import DirectionsRun from "@mui/icons-material/DirectionsRun";
import DirectionsBike from "@mui/icons-material/DirectionsBike";
import Pool from "@mui/icons-material/Pool";
import FitnessCenter from "@mui/icons-material/FitnessCenter";
import type { Sport } from "./format";
export function SportIcon({ sport }: { sport: Sport }) {
  const Icon =
    sport === "RUN"
      ? DirectionsRun
      : sport === "SWIM"
        ? Pool
        : sport === "RIDE"
          ? DirectionsBike
          : FitnessCenter;
  return <Icon aria-hidden="true" />;
}
