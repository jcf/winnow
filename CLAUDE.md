# Winnow

Tailwind CSS class merging library for Clojure. Resolves conflicts by keeping the last value per conflict group.

## Quick Start

```sh
just test     # Run tests
just lint     # Run clj-kondo
just fmt      # Format with treefmt
just conform  # Run conformance tests (requires pnpm in conformance/)
just bench    # Run benchmarks
just docs     # Regenerate doc/supported-classes.org
```

## Project Structure

```
src/winnow/
  api.clj       # Public API: resolve, make-resolver, supported-patterns
  classify.clj  # Class -> group classification
  config.clj    # All Tailwind utilities, conflicts, colors
  parse.clj     # Class string parsing (modifiers, arbitrary values)
  trie.clj      # Trie for O(n) prefix matching

dev/winnow/
  doc.clj       # Documentation generation (not shipped)

test/winnow/
  api_test.clj         # API unit tests
  generative_test.clj  # Property-based tests (clojure.test.check)
  parse_test.clj       # Parser unit tests
  conformance_test.clj # Conformance suite from spec/winnow.txt

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

Tests run via Kaocha with `:dev:test:runner` aliases:

```sh
just test              # All tests
just test --fail-fast  # Stop on first failure
```

### Test Types

1. **Unit tests** - Direct API testing
2. **Conformance tests** - 266 test cases in `spec/winnow.txt`
3. **Generative tests** - Property-based tests using `defspec`

### Conformance Format

Each line in `spec/winnow.txt`: `input → expected`

```
px-2 px-4 → px-4
hover:p-2 hover:p-4 → hover:p-4
```

## Linting

```sh
just lint  # clj-kondo
```

clj-kondo config: `.clj-kondo/config.edn`

## Key Files

| File                        | Purpose                                              |
| --------------------------- | ---------------------------------------------------- |
| `config.clj`                | All supported utilities - modify here to add classes |
| `spec/winnow.txt`           | Conformance tests - add test cases here              |
| `doc/supported-classes.org` | Generated reference of all supported patterns        |
| `justfile`                  | Task runner commands                                 |
| `deps.edn`                  | Dependencies and aliases                             |

## Adding Support for New Classes

1. Add to `:exact` map in `config.clj` for exact matches
2. Add to `:prefixes` map for prefix patterns (e.g., `p-*`)
3. Add conflict relationships in `:conflicts` if needed
4. Add conformance tests to `spec/winnow.txt`
5. Run `just test` to verify
6. Run `just docs` to regenerate reference

## Listing Supported Patterns

```clojure
(require '[winnow.api :as winnow])
(winnow/supported-patterns)
;; => {:exact #{...}, :prefixes #{...}, :colors #{...}}
```

## Regenerating Documentation

After modifying `config.clj`, regenerate the reference:

```sh
just docs
```

This runs `clojure -X:doc` which invokes `winnow.doc/reference`.
