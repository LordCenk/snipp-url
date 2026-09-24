/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Backend origin when it is not served from the same origin, e.g. https://api.example.com */
  readonly VITE_API_BASE_URL?: string;
  /** Origin used to build public short links; defaults to the API origin */
  readonly VITE_SHORT_URL_BASE?: string;
}
