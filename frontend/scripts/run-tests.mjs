import { spawnSync } from 'node:child_process'
import { readFileSync, rmSync, statSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const startedAt = Date.now()

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

let report
try {
  if (statSync(reportPath).mtimeMs < startedAt) {
    fail(`${reportPath} predates this run; refusing to validate a stale report.`)
  }
  report = JSON.parse(readFileSync(reportPath, 'utf8'))
} catch {
  fail(`vitest exited 0 without leaving a readable ${reportPath}.`)
}

const pending = report.numPendingTests ?? 0
const todo = report.numTodoTests ?? 0

if (pending + todo > 0) {
  fail(
    `${pending + todo} test(s) did not execute (${pending} pending, ${todo} todo). A .skip or .todo ` +
      'removes coverage while the suite stays green; remove it, or delete the test outright.',
  )
}

console.log(`Test run guard: ${report.numPassedTests ?? 0} tests passed, none skipped or todo.`)
