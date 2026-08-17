const fsModuleName = 'node:' + 'fs'
const urlModuleName = 'node:' + 'url'

interface MinimalFsModule {
  readFileSync: (path: string, encoding: string) => string
}

interface MinimalUrlModule {
  fileURLToPath: (url: object) => string
  URL: new (input: string, base: string) => object
}

export async function readSourceFile(relativePathFromThisFile: string, importMetaUrl: string): Promise<string> {
  const fsModule = (await import(fsModuleName)) as unknown as MinimalFsModule
  const urlModule = (await import(urlModuleName)) as unknown as MinimalUrlModule
  const resolvedPath = urlModule.fileURLToPath(new urlModule.URL(relativePathFromThisFile, importMetaUrl))
  return fsModule.readFileSync(resolvedPath, 'utf-8')
}
