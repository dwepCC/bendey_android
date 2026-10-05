/**
 * Copia el centro de ayuda exportado por Tauri a los assets de Android (R10.7): MISMOS artículos.
 *
 *   node scripts/sync-help.mjs          copia help.json de Tauri a feature/ayuda/src/main/assets/help/
 *   node scripts/sync-help.mjs --check  falla (exit 1) si la copia de Android no coincide con la de Tauri
 *
 * La fuente es `docs/help-export/help.json` del repo front_tenant_restaurant_tauri (se regenera allá con
 * `npm run help:export`). Si el repo de Tauri no está al lado, --check no falla (no hay con qué comparar).
 * Ruta de Tauri alternativa: variable de entorno TAURI_REPO.
 */
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const TAURI = resolve(process.env.TAURI_REPO ?? join(ROOT, '..', 'front_tenant_restaurant_tauri'))
const SOURCE = join(TAURI, 'docs', 'help-export', 'help.json')
const TARGET = join(ROOT, 'feature', 'ayuda', 'src', 'main', 'assets', 'help', 'help.json')
const CHECK = process.argv.includes('--check')

const normalize = (text) => text.replace(/\r\n/g, '\n')

if (!existsSync(SOURCE)) {
  console.log(`No encuentro ${SOURCE}: ${CHECK ? 'no hay con qué comparar.' : 'nada que copiar.'}`)
  process.exit(CHECK ? 0 : 1)
}

const source = normalize(readFileSync(SOURCE, 'utf8'))
const target = existsSync(TARGET) ? normalize(readFileSync(TARGET, 'utf8')) : null

if (CHECK) {
  if (source === target) {
    console.log('La ayuda de Android coincide con la de Tauri.')
    process.exit(0)
  }
  console.error('La ayuda de Android NO coincide con la de Tauri. Corre: node scripts/sync-help.mjs')
  process.exit(1)
}

mkdirSync(dirname(TARGET), { recursive: true })
writeFileSync(TARGET, source, 'utf8')
const parsed = JSON.parse(source)
console.log(`Copiado: ${parsed.articles.length} artículos, ${parsed.categories.length} categorías (versión ${parsed.version}).`)
