# Winnow 1.0 Code Review Summary

## Overview

Comprehensive code review in preparation for 1.0 release. The codebase is
well-structured with clear separation of concerns.

## Key Findings

### High Priority

| Issue                             | Location                                          | Description                                                          |
| --------------------------------- | ------------------------------------------------- | -------------------------------------------------------------------- |
| Duplicate functions               | parse.cljc, trie.cljc, classify.cljc, config.cljc | `char-at`, `str-len`, `arbitrary?`, `content` defined multiple times |
| Public function should be private | config.cljc:24                                    | `stroke-width?` is public but internal                               |

### Medium Priority

| Issue                       | Location         | Description                                                 |
| --------------------------- | ---------------- | ----------------------------------------------------------- |
| Inefficient merge loop      | api.cljc:118-140 | Uses explicit indexing; `reduce` with `rseq` more idiomatic |
| Hardcoded postfix conflicts | api.cljc:84-87   | `:text-size`/`:leading` relationship hardcoded              |

### Low Priority

| Issue               | Location            | Description                               |
| ------------------- | ------------------- | ----------------------------------------- |
| Empty keyword set   | classify.cljc:26-27 | `border-width-keywords` is `#{}`          |
| Input normalization | api.cljc            | No lenient input handling for convenience |

## Phases

1. **Benchmark infrastructure** - Machine-readable output for comparisons
2. **Extract to parse.cljc** - Make shared functions public, add tests
3. **Optimize merge** - Refactor `merge-classes` to use `reduce` with `rseq`
4. **Data-driven config** - Move hardcoded relationships into config
5. **API enhancements** - Add `normalize` function
6. **Cleanup** - Fix visibility, remove dead code

## Workflow

Before starting phase 2:

```sh
just bench > bench/baseline.edn
```

After each phase:

```sh
just                    # Must pass
just bench > bench/after-phase-N.edn
```

Compare with a script or manual EDN diff.

## API Design: Input Normalization

**Decision**: Add `winnow.api/normalize` as a composable function.

- Keeps `resolve` strict (fails fast on bad input)
- Users opt into leniency: `(comp resolve normalize)`
- No hidden coercion surprises

```clojure
(def my-resolve (comp winnow/resolve winnow/normalize))
(my-resolve nil)              ;; => ""
(my-resolve "p-4 m-2")        ;; => "p-4 m-2"
```
