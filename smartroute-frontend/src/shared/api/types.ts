/**
 * Tipos da API, derivados do contrato (contracts/api/openapi.yaml, ADR 0009).
 * `schema.d.ts` é gerado por `npm run generate:api`: não edite à mão. Mudou a API? Edite o contrato e
 * gere de novo; o CI falha se `schema.d.ts` estiver desatualizado.
 */
import type { components } from './schema'

type Schemas = components['schemas']

export type TripStatus = Schemas['TripStatus']
export type Location = Schemas['Location']
export type Trip = Schemas['Trip']
export type TripPage = Schemas['TripPage']
export type CreateTripRequest = Schemas['TripRequest']
export type GeocodeResult = Schemas['GeocodeResult']
export type AuditEntry = Schemas['AuditEntry']
export type AuditEntryPage = Schemas['AuditEntryPage']
/** Corpo de erro RFC 7807 devolvido pelo backend. */
export type Problem = Schemas['Problem']
