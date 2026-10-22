## ADDED Requirements

### Requirement: Minimum length follows authentication factors
When a password is established or replaced, the system MUST require at least 15 characters when the password is the only authentication factor, and at least 8 characters when it is used with MFA.

#### Scenario: Password is the only factor
- WHEN a user submits a password with fewer than 15 characters
- THEN the password is rejected
- WHEN the password has at least 15 characters and is not blocked
- THEN the password satisfies the length requirement

#### Scenario: Password is used with MFA
- WHEN a user submits a password with fewer than 8 characters
- THEN the password is rejected
- WHEN the password has at least 8 characters and is not blocked
- THEN the password satisfies the length requirement

### Requirement: Accept passphrases without composition rules
The system MUST allow letters, numbers, spaces, and symbols and MUST NOT require a specific combination of character types.

#### Scenario: Password contains spaces or symbols
- WHEN a password meets the applicable minimum and contains spaces or symbols
- THEN it is not rejected solely for those characters

#### Scenario: Password lacks a prescribed character type
- WHEN a password meets the applicable minimum and is not blocked
- THEN it is not rejected for lacking an uppercase letter, number, or symbol

### Requirement: Reject common or compromised passwords
The system MUST reject passwords identified as common or compromised, regardless of whether they meet the applicable minimum length.

#### Scenario: Blocked password meets minimum length
- WHEN a password meets the applicable minimum but is identified as common or compromised
- THEN the password is rejected

### Requirement: Preserve long passwords without truncation
The system MUST accept passwords of at least 64 characters when they meet the other policy requirements and MUST NOT silently truncate them.

#### Scenario: Password has at least 64 characters
- WHEN a password of 64 or more characters meets the applicable minimum and is not blocked
- THEN the complete password is accepted without silent truncation
