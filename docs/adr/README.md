# Architecture Decision Records (ADR)

## What is an ADR?

An **Architecture Decision Record (ADR)** is a short document that captures an important architectural decision made along with its context and consequences. ADRs are living documents that evolve with the project.

## Why Use ADRs?

1. **Documentation**: Captures the "why" behind architectural choices
2. **History**: Creates a searchable record of decisions over time
3. **Context**: Helps future developers understand the reasoning
4. **Collaboration**: Facilitates discussion and review of architectural decisions
5. **Onboarding**: Speeds up understanding for new team members

## When to Create an ADR

Every significant subsystem or architectural decision requires an ADR. Specifically:

- ✅ New package or module creation
- ✅ New external dependency
- ✅ Changes to data storage strategy
- ✅ Changes to API design
- ✅ Non-obvious technical decisions
- ✅ Rejection of alternative approaches

## ADR Format

Each ADR must include:

| Section | Purpose |
|---------|---------|
| **Title** | Clear, descriptive name of the decision |
| **Status** | `Proposed`, `Accepted`, `Deprecated`, or `Superseded` |
| **Context** | What problem are we solving? |
| **Decision** | What did we decide to do? |
| **Alternatives Considered** | What else did we evaluate? Why not? |
| **Consequences** | What are the trade-offs? |
| **Dependencies** | What does this module use/provide? |
| **Public API** | What interfaces are exposed? |
| **Future Work** | What remains to be done? |

## ADR Naming Convention

```
ADR-{NUMBER}-{short-name}.md
```

Examples:
- `ADR-0001-framework.md`
- `ADR-0002-logging.md`
- `ADR-0003-driver-detection.md`
- `ADR-0004-driver-download.md`

The number follows the task/issue number that resulted in the decision.

## Project ADRs

| ADR | Subsystem | Status | Summary |
|-----|-----------|--------|---------|
| [ADR-0003](ADR-0003-driver-detection.md) | Driver Detection Engine | Accepted | Detecting Edge browser and WebDriver installation |
| [ADR-0004](ADR-0004-driver-download.md) | Driver Download Engine | Accepted | Automatic WebDriver downloading |

## How to Create a New ADR

1. **Identify the Decision**: Determine what architectural decision needs documentation
2. **Create File**: Create `docs/adr/ADR-{NUMBER}-{short-name}.md`
3. **Fill Template**: Complete all sections from the format above
4. **Document Alternatives**: Always include at least two alternatives and explain rejection
5. **Link Dependencies**: Note what other subsystems depend on this module
6. **Plan Future Work**: Include what remains to be implemented
7. **Review**: Submit for review with the PR that implements the decision

## Example: When to Reject an Alternative

❌ **Bad Alternative Documentation:**
```markdown
### Alternatives Considered

We considered using Library X but didn't.
```

✅ **Good Alternative Documentation:**
```markdown
### Alternatives Considered

#### Alternative 1: WebDriverManager Library

This popular library handles driver detection and downloading automatically.

**Rejected because:**
- Adds a third-party dependency that must be maintained
- Hides control over download source and storage location
- The project requirements explicitly prohibit external driver management libraries
- Would require refactoring to integrate properly

#### Alternative 2: Manual Driver Configuration

Require users to manually download and place drivers.

**Rejected because:**
- Poor user experience - automation is a key requirement
- Users may download incompatible versions
- Adds complexity and potential for user errors
```

## Maintenance

- ADRs are **living documents**
- Update ADRs when circumstances change
- Mark superseded decisions with `Status: Superseded` and link to replacement
- Add `Superseded by ADR-{number}` to the header

## References

- [Documenting Architecture Decisions - Michael Nygard](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
- [Markdown Any Decision Records (MADR)](https://adr.github.io/madr/)

## Contributing

When implementing a new subsystem:

1. Create the ADR **before** or **alongside** implementation
2. Include the ADR in the same PR as the implementation
3. Ensure ADR number matches task/issue number
4. Update this README when adding new ADRs
