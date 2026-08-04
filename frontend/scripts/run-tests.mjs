import { spawnSync } from 'node:child_process'
import { readFileSync, rmSync, statSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const startedAt = Date.now()

const MINIMUM_TEST_FILES = 14
const MINIMUM_TESTS = 156

const reportPath = fileURLToPath(new URL('../test-results.json', import.meta.url))
const vitestBin = fileURLToPath(
  new URL(
    `../node_modules/.bin/vitest${process.platform === 'win32' ? '.cmd' : ''}`,
    import.meta.url,
  ),
)

function fail(message) {
  console.error(`Test run guard failed: ${message}`)
  process.exit(1)
}

rmSync(reportPath, { force: true })

const run = spawnSync(
  vitestBin,
  [
    'run',
    '--reporter=default',
    '--reporter=json',
    `--outputFile.json=${reportPath}`,
    ...process.argv.slice(2),
  ],
  { stdio: 'inherit', shell: process.platform === 'win32' },
)

if (run.error) {
  fail(`could not start vitest (${run.error.message}).`)
}

if (run.status !== 0) {
  process.exit(run.status === null ? 1 : run.status)
}

let stats
try {
  stats = statSync(reportPath)
} catch {
  fail(`vitest exited 0 without writing ${reportPath}.`)
}

if (stats.mtimeMs < startedAt) {
  fail(`${reportPath} predates this run; refusing to validate a stale report.`)
}

let report
try {
  report = JSON.parse(readFileSync(reportPath, 'utf8'))
} catch {
  fail(`${reportPath} is not readable JSON.`)
}

const files = Array.isArray(report.testResults) ? report.testResults.length : 0
const passed = typeof report.numPassedTests === 'number' ? report.numPassedTests : 0
const pending = typeof report.numPendingTests === 'number' ? report.numPendingTests : 0
const todo = typeof report.numTodoTests === 'number' ? report.numTodoTests : 0
const failed = typeof report.numFailedTests === 'number' ? report.numFailedTests : 0
const notExecuted = pending + todo

if (failed > 0) {
  fail(
    `${failed} test(s) failed even though vitest exited 0. Fix the failing test(s); the report is the ` +
      'source of truth here, and an exit code that disagrees with it is itself a bug worth reporting upstream.',
  )
}

if (report.success !== true) {
  fail(
    'vitest exited 0 but the report is not marked successful, which usually means a test file failed to ' +
      'collect (import error, syntax error) rather than a test failing. Run `npm test` and read the vitest ' +
      'output above this line for the file that would not load.',
  )
}

if (notExecuted > 0) {
  fail(
    `${notExecuted} test(s) did not execute (${pending} pending, ${todo} todo). A .skip, .only or .todo ` +
      'removes coverage while the suite stays green; remove it, or lower the minimums deliberately.',
  )
}

if (passed < MINIMUM_TESTS || files < MINIMUM_TEST_FILES) {
  fail(
    `${passed} tests passed in ${files} files, expected at least ${MINIMUM_TESTS} tests in ` +
      `${MINIMUM_TEST_FILES} files. Either tests stopped being collected, or the minimums need lowering deliberately.`,
  )
}

console.log(`Test run guard: ${passed} tests passed in ${files} files, none skipped or todo.`)
