/** Error body returned by user-service, catalog-service and loan-service */
export type ApiErrorResponse = {
  status: number
  message: string
  /** validation errors as "field: message" (empty otherwise) */
  details: string[]
}

/** Error body returned by the API Gateway itself (service down or not registered) */
export type GatewayErrorResponse = {
  timestamp: string
  path: string
  status: number
  error: string
  requestId: string
}
