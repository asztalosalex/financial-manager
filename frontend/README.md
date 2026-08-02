# Financial Manager — Frontend

React 19 + TypeScript + Vite single-page app for the Financial Manager backend.

## Scripts

| Command | Purpose |
| --- | --- |
| `npm run dev` | Vite dev server with HMR |
| `npm run build` | Type-check (`tsc --noEmit`) then production build |
| `npm run typecheck` | Type-check only |
| `npm run lint` | ESLint over the repo |
| `npm test` | **The sanctioned test entry point.** See below |
| `npm run test:watch` | Interactive vitest watch mode, for development only |

## Testing

`npm test` is the only sanctioned way to run the suite. It does not call vitest
directly — it runs `scripts/run-tests.mjs`, which wraps vitest and then validates
the JSON report vitest wrote. The wrapper fails the run if any of the following
is true:

- vitest itself exited non-zero;
- `test-results.json` is missing, unreadable, or older than the current run
  (a stale report must never be mistaken for a fresh pass);
- the report contains failed tests, or is not marked `success` — which catches a
  test file that failed to *collect* (an import or syntax error), as opposed to a
  test that failed to *pass*;
- the report contains pending tests, i.e. anything reached by `.skip` or excluded
  by `.only`;
- fewer than **58 passed tests across 9 test files** were collected.

The floor is a tripwire, not a target. Adding tests pushes the real count above
58, which is fine and needs no edit. Lower it only deliberately, as part of a
change that removes tests on purpose.

### Do not run `npx vitest run` to verify a change

Bare vitest **bypasses every check above**. Most importantly, `describe.skip` or
`it.skip` makes vitest print `Tests 50 passed | 8 skipped (58)` and still **exit
0** — green to any CI step or shell habit that only reads the exit code, while
eight tests have silently stopped protecting anything. That exact hole is why
`run-tests.mjs` exists. CI and any pre-merge check must call `npm test`.

If you want the fast interactive loop that usually motivates reaching for bare
vitest, use `npm run test:watch`. Watch mode is for development; before handing
work off or merging, run `npm test`.

## Structure

```
src/
  api/         typed API clients, error mapping
  auth/        auth context, session hooks, redirect sanitisation
  components/  shared presentational components
  pages/       routed screens
  test/        test setup and shared fixtures
```
