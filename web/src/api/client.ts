import {
  AccountApi,
  ProfilesApi,
  SocialApi,
  SharedActivitiesApi,
  Configuration,
  PrivacyApi,
  ResponseError,
  WorkoutsApi,
} from "./generated";

const config = new Configuration({
  basePath: "",
  middleware: [
    {
      pre: async ({ url, init }) => {
        if (
          init.method &&
          !["GET", "HEAD", "OPTIONS"].includes(init.method.toUpperCase())
        ) {
          const response = await fetch("/api/v1/session/csrf", {
            credentials: "same-origin",
            signal: AbortSignal.timeout(10_000),
          });
          if (!response.ok)
            throw new Error("Could not refresh your session. Try again.");
          const csrf: unknown = await response.json();
          if (
            !csrf ||
            typeof csrf !== "object" ||
            !("headerName" in csrf) ||
            !("token" in csrf) ||
            typeof csrf.headerName !== "string" ||
            typeof csrf.token !== "string"
          )
            throw new Error("Invalid session response. Refresh this page.");
          const headers = new Headers(init.headers);
          headers.set(csrf.headerName, csrf.token);
          return {
            url,
            init: { ...init, headers, credentials: "same-origin" },
          };
        }
        return { url, init };
      },
    },
  ],
});
export const accountApi = new AccountApi(config);
export const workoutsApi = new WorkoutsApi(config);
export const privacyApi = new PrivacyApi(config);
export const profilesApi = new ProfilesApi(config);
export const socialApi = new SocialApi(config);
export const sharedActivitiesApi = new SharedActivitiesApi(config);

export async function apiProblem(error: unknown): Promise<{
  message: string;
  fields: Record<string, string>;
  status?: number;
}> {
  if (error instanceof ResponseError) {
    if (error.response.status === 401)
      return {
        message:
          "Your session expired. Your entered values are kept. Sign in again before retrying.",
        fields: {},
        status: 401,
      };
    let body: unknown;
    try {
      body = await error.response.clone().json();
    } catch {
      /* A proxy outage can return HTML. */
    }
    if (body && typeof body === "object") {
      const detail =
        "detail" in body && typeof body.detail === "string"
          ? body.detail
          : "The request could not be completed. Please try again.";
      const fields: Record<string, string> = {};
      if ("errors" in body && body.errors && typeof body.errors === "object") {
        for (const [key, value] of Object.entries(body.errors))
          if (typeof value === "string") fields[key] = value;
      }
      return { message: detail, fields, status: error.response.status };
    }
  }
  return {
    message:
      "Could not reach Syla. Your entered values are kept; retry when the connection returns.",
    fields: {},
  };
}
