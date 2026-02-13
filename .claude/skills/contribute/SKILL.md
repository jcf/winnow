---
name: contribute
description: Guide for contributing to Winnow. Use when adding features, fixing bugs, or understanding the codebase.
---

# Winnow Contributor Guide

You're contributing to Winnow, a Tailwind CSS class merging library for Clojure.

## Quick Reference

```sh
just test     # Run all tests
just lint     # Run clj-kondo
just docs     # Regenerate doc/supported-classes.org
```

## Key Files

| File                              | Purpose                |
| --------------------------------- | ---------------------- |
| `src/winnow/config.clj`           | All Tailwind utilities |
| `spec/winnow.txt`                 | Conformance tests      |
| `test/winnow/generative_test.clj` | Property-based tests   |

## Adding a New Tailwind Class

1. **Determine the type**:
   - Exact match (e.g., `block`, `hidden`) → add to `:exact` map
   - Prefix pattern (e.g., `p-*`, `bg-*`) → add to `:prefixes` map

2. **Edit `src/winnow/config.clj`**:

   ```clojure
   ;; Exact match
   :exact {"new-class" :group-name}

   ;; Prefix pattern
   :prefixes {"new-prefix" {:group :group-name}}
   ```

3. **Add conflicts if needed** (in `:conflicts` map)

4. **Add conformance test** to `spec/winnow.txt`:

   ```
   old-class new-class
   => new-class
   ```

5. **Run tests and regenerate docs**:
   ```sh
   just test && just docs
   ```

## Code Style

- No comments explaining code
- No docstrings unless essential
- Sort keys/values alphabetically
- Align values in maps
- Use `;;;` section headers

## Conformance Test Format

```
# Comment
input-class-a input-class-b
=> expected-output

# Multiple inputs with |
set1-a set1-b | set2-a set2-b
=> merged-result
```

## Property-Based Testing

Tests in `generative_test.clj` verify invariants:

- Idempotence: `resolve(resolve(x)) = resolve(x)`
- Output ⊆ Input
- Modifier independence
- Conflict symmetry

## Before Submitting

1. `just lint` passes
2. `just test` passes
3. `just docs` regenerated if config changed
4. Commits are atomic with clear messages
