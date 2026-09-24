export default function Logo({ size = 24 }: { size?: number }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" aria-hidden="true">
      <rect width="32" height="32" rx="8" fill="var(--accent)" />
      <path
        d="M13.5 18.5l5-5M11 15.5l-1.8 1.8a3.5 3.5 0 005 5l1.8-1.8M21 16.5l1.8-1.8a3.5 3.5 0 00-5-5L16 11.5"
        stroke="#fff"
        strokeWidth="2.2"
        fill="none"
        strokeLinecap="round"
      />
    </svg>
  );
}
