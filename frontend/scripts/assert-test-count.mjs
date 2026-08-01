import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const MINIMUM_TEST_FILES = 9
const MINIMUM_TESTS = 56

const reportPath = fileURLToPath(new URL('../test-results.json', import.meta.url))

let report
try {
  report = JSON.parse(readFileSync(reportPath, 'utf8'))
} catch {
  console.error(`Test count guard: no vitest json report found at ${reportPath}.`)
  process.exit(1)
}

const collectedFiles = Array.isArray(report.testResults) ? report.testResults.length : 0
const collectedTests = typeof report.numTotalTests === 'number' ? report.numTotalTests : 0

if (collectedFiles < MINIMUM_TEST_FILES || collectedTests < MINIMUM_TESTS) {
  console.error(
    `Test count guard failed: collected ${collectedTests} tests in ${collectedFiles} files, ` +
      `expected at least ${MINIMUM_TESTS} tests in ${MINIMUM_TEST_FILES} files. ` +
      'Either files stopped being collected, or the minimums need lowering deliberately.',
  )
  process.exit(1)
}

console.log(`Test count guard: ${collectedTests} tests collected in ${collectedFiles} files.`)
