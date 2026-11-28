import js from '@eslint/js'
import globals from 'globals'
import reactHooks from 'eslint-plugin-react-hooks'
import reactRefresh from 'eslint-plugin-react-refresh'
import tseslint from 'typescript-eslint'
import { defineConfig, globalIgnores } from 'eslint/config'
import { readdirSync } from 'node:fs'

// Funcionalidades em src/features/<nome>: cada uma expõe a API pública no index.ts
const features = readdirSync(new URL('./src/features', import.meta.url), { withFileTypes: true })
  .filter((entry) => entry.isDirectory())
  .map((entry) => entry.name)

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      js.configs.recommended,
      tseslint.configs.recommended,
      reactHooks.configs.flat.recommended,
      reactRefresh.configs.vite,
    ],
    languageOptions: {
      ecmaVersion: 2022,
      globals: globals.browser,
    },
  },
  {
    // Uma funcionalidade usa outra só pelo index.ts dela (../../geocoding), nunca por arquivo interno
    files: ['src/features/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': ['error', {
        patterns: [{
          regex: `^(\\.\\./)+(features/)?(${features.join('|')})/.+`,
          message: 'Importe a outra funcionalidade pelo index.ts dela (por exemplo ../../geocoding), não por arquivo interno.',
        }],
      }],
    },
  },
  {
    // shared é a base: não depende de nenhuma funcionalidade
    files: ['src/shared/**/*.{ts,tsx}'],
    rules: {
      'no-restricted-imports': ['error', {
        patterns: [{
          regex: '(^|/)features(/|$)',
          message: 'shared não pode depender de src/features; mova o código para a funcionalidade.',
        }],
      }],
    },
  },
])
