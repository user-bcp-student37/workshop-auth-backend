# Spec Delta

## Purpose
Define observable login error behavior so persistence failures are distinguishable from credential mismatches without exposing internal storage details.

## ADDED Requirements

### Requirement: Distinguish persistence failures from invalid credentials
The system MUST report a generic internal error when credential verification cannot complete because of a persistence failure, rather than reporting invalid credentials.

#### Scenario: Credential lookup completes with no matching user
- **WHEN** credential verification completes successfully and no user matches the submitted username and password
- **THEN** `POST /auth/login` returns `401 Unauthorized` with the existing generic invalid-credentials response

#### Scenario: Credential lookup cannot complete because persistence failed
- **WHEN** `POST /auth/login` cannot complete credential verification because persistent storage failed
- **THEN** it returns `500 Internal Server Error` with the existing generic internal-error response
- **AND** the response does not expose database, driver, table, or exception details

#### Scenario: Credential lookup completes successfully
- **WHEN** credential verification completes successfully and the credentials match a user
- **THEN** `POST /auth/login` retains its existing `200 OK` response contract