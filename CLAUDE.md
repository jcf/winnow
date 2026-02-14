# Winnow

Tailwind CSS class merging library for Clojure. Resolves conflicts by keeping the last value per conflict group.

## Quick Start

```sh
just          # Run full test suite (REQUIRED before committing)
just fmt      # Format with treefmt
just bench    # Run benchmarks
just docs     # Regenerate doc/supported-classes.org
```

**Important**: Always run `just` (no arguments) to execute the full test suite. Running `just test` alone is insufficient - it only runs Clojure tests and misses other critical checks.

## Project Structure

```
src/winnow/
  api.cljc       # Public API: resolve, make-resolver, supported-patterns
  classify.cljc  # Class -> group classification
  config.cljc    # All Tailwind utilities, conflicts, colors
  parse.cljc     # Class string parsing (modifiers, arbitrary values)
  trie.cljc      # Trie for O(n) prefix matching

dev/winnow/
  doc.clj        # Documentation generation (not shipped)
  hygiene.clj    # Reflection/boxing warning checker

test/winnow/
  api_test.cljc         # API unit tests (runs on Clojure, ClojureScript, Babashka)
  parse_test.cljc       # Parser unit tests
  generative_test.cljc  # Property-based tests (clojure.test.check)
  conformance_test.clj  # Conformance suite from spec/winnow.txt (Clojure only)
  spec_test.clj         # clojure.spec validation (Clojure only)

doc/
  supported-classes.org  # Generated reference of all supported patterns

spec/winnow.txt          # Conformance test cases
bench/winnow/bench.clj   # Criterium benchmarks
```

## Code Conventions

- No docstrings unless they add significant value beyond the function name
- No comments explaining code; use git history for context
- Sort values where order is insignificant (maps, sets)
- Align associatives in let forms and maps
- Use `;;;` section headers to separate logical groups
- Prefer data over functions over macros
- Separate pure operations from IO

## Testing

**Always run `just` (no arguments) for the full test suite.** This executes:

1. `just lint` - clj-kondo static analysis
2. `just test` - Clojure tests via Kaocha
3. `just hygiene` - Reflection and boxed math warnings (must pass with zero warnings)
4. `just test-bb` - Babashka compatibility tests
5. `just test-cljs` - ClojureScript tests via Node.js

All five must pass before committing.

### Individual Test Commands

```sh
just                   # Full suite (required)
just test              # Clojure tests only
just test --fail-fast  # Stop on first failure
just test-bb           # Babashka tests
just test-cljs         # ClojureScript tests
just hygiene           # Check for reflection/boxing warnings
just lint              # clj-kondo
```

### Test Types

| Type | Location | Description |
|------|----------|-------------|
| Unit tests | `test/winnow/*_test.cljc` | Direct API testing |
| Conformance tests | `spec/winnow.txt` | 278 test cases from tailwind-merge |
| Generative tests | `test/winnow/generative_test.cljc` | Property-based tests via test.check |
| Spec tests | `test/winnow/spec_test.clj` | clojure.spec validation |

### Conformance Format

Each line in `spec/winnow.txt`: `input → expected`

```
px-2 px-4 → px-4
hover:p-2 hover:p-4 → hover:p-4
```

### Hygiene Requirements

Code must compile without reflection or boxed math warnings. The `just hygiene` task checks this. Common fixes:
- Add `^String` type hints for string operations
- Use `(long x)` to avoid boxed math in loops
- Use reader conditionals (`#?(:clj ...)`) for Java interop

## Linting

```sh
just lint  # clj-kondo
```

clj-kondo config: `.clj-kondo/config.edn`

## Key Files

| File                        | Purpose                                              |
| --------------------------- | ---------------------------------------------------- |
| `config.cljc`               | All supported utilities - modify here to add classes |
| `classify.cljc`             | Classification logic - validators and grouping       |
| `spec/winnow.txt`           | Conformance tests - add test cases here              |
| `doc/supported-classes.org` | Generated reference of all supported patterns        |
| `justfile`                  | Task runner commands                                 |
| `deps.edn`                  | Dependencies and aliases                             |

## Adding Support for New Classes

1. Add to `:exact` map in `config.cljc` for exact matches
2. Add to `:prefixes` map for prefix patterns (e.g., `p-*`)
3. Add conflict relationships in `:conflicts` if needed
4. Add conformance tests to `spec/winnow.txt`
5. Run `just` to verify (full test suite)
6. Run `just docs` to regenerate reference

## Listing Supported Patterns

```clojure
(require '[winnow.api :as winnow])
(winnow/supported-patterns)
;; => {:exact #{...}, :prefixes #{...}, :colors #{...}}
```

## Regenerating Documentation

After modifying `config.cljc`, regenerate the reference:

```sh
just docs
```

This runs `clojure -M:dev -m winnow.doc` which generates `doc/supported-classes.org`.
