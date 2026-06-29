# Agent Instructions for ParkingLotEventGenerator

These instructions apply to any coding agent working in this repository.

## Core Rules

1. Do not execute terminal commands by default.
2. If a command is absolutely necessary (for example: verifying a build, running tests, or inspecting Git state), ask the user first and wait for explicit approval.
3. Prefer static analysis of source files and configuration over command execution.
4. When asking to run a command, include:
   - the exact command,
   - why it is needed,
   - expected impact,
   - whether it changes files.
5. If command approval is not granted, continue with a best-effort code-only solution and clearly call out what could not be validated.

## Editing Guidance

- Keep changes minimal and targeted.
- Do not revert or overwrite unrelated local changes.
- Preserve existing project conventions (Java style, package structure, naming).

## Response Expectations

- Briefly explain what changed and why.
- List any assumptions.
- If validation was not run due to command restrictions, explicitly state that.

