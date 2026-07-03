# AI Agent Guidelines

This file defines how AI coding agents should review and contribute to this project.

## Correctness First

Prioritize correctness over cleverness. Make sure the implementation satisfies the business
requirements before suggesting style or structure improvements.

## Keep Changes Focused

Prefer small, targeted changes. Avoid broad refactors or unrelated cleanup unless they clearly
reduce risk or significantly improve maintainability.

## Favor Readability

Use simple, readable, and maintainable code. Prefer straightforward control flow and explicit
behavior over clever abstractions.

## Preserve Project Style

Follow the existing architecture, package structure, naming conventions, formatting, and testing
patterns. New code should feel consistent with the rest of the project.

## Review For Real Bugs

During reviews, prioritize correctness issues before style improvements. Look for logic bugs,
edge cases, null handling issues, race conditions, broken error handling, and performance problems.

## Consider Security

When applicable, consider security implications such as validation gaps, unsafe input handling,
data exposure, injection risks, and incorrect authorization assumptions.

## Validate Requirements

Check that the implemented behavior fully covers the stated business requirements and documented
assumptions. If something is ambiguous, state the assumption explicitly instead of guessing.

## Testing Expectations

Suggest missing or improved unit, integration, or end-to-end tests when they would increase
confidence in important behavior, edge cases, or regressions.

## Avoid Unnecessary Style Comments

Do not suggest stylistic changes unless they improve clarity, maintainability, or consistency in a
meaningful way. Avoid churn that does not reduce risk.

## Explain Reasoning

Explain the reasoning behind review comments and proposed changes. Be specific about the scenario,
input, or requirement that motivates the comment.

## Propose Working Code

When proposing code, make sure it should compile, follows project conventions, and integrates with
the existing design. Prefer minimal examples over incomplete rewrites.

## Be Explicit About Uncertainty

If requirements, behavior, or context are unclear, call that out directly. Do not silently invent
requirements or assume behavior that is not documented.
