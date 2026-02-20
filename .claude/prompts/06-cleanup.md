# Phase 6: Cleanup

## Goal

Fix minor issues identified in code review.

## Issue 1: Public function should be private

### Location

`config.cljc:24`

### Current

```clojure
(defn stroke-width?
  [s]
  ...)
```

### Fix

```clojure
(defn- stroke-width?
  [s]
  ...)
```

## Issue 2: Empty keyword set

### Location

`classify.cljc:26-27`

### Current

```clojure
(def ^:private border-width-keywords
  #{})
```

### Analysis

The `border-width?` function works without it:

```clojure
(defn- border-width?
  [^String s]
  (or (arbitrary? s)
      (variable? s)
      (parse-long s)
      (border-width-keywords s)))  ; always false
```

### Fix

Remove the empty set and simplify the function:

```clojure
(defn- border-width?
  [^String s]
  (or (parse/arbitrary? s)
      (parse/variable? s)
      (parse-long s)))
```

## Issue 3: Add algorithm comment to sort-modifiers

### Location

`api.cljc:57-73`

### Current

No explanation of what the function does.

### Fix

Add a docstring:

```clojure
(defn- sort-modifiers
  "Sorts modifiers alphabetically while preserving the relative position of
  order-sensitive modifiers (group-*, peer-*, [*], *:).

  For example:
    [\"hover\" \"sm\" \"group-focus\"] -> [\"group-focus\" \"hover\" \"sm\"]

  The group-focus stays in position 0, while hover and sm are sorted."
  [modifiers order-sensitive]
  ...)
```

## Issue 4: Verify public API surface

Check that only intended functions are public:

### winnow.api (public)

- `resolve` ✓
- `make-resolver` ✓
- `supported-patterns` ✓
- `normalize` ✓ (after Phase 5)

### winnow.config (internal)

- `default` - currently public, should it be?

### Decision

Keep `config/default` public - advanced users may want to inspect it.
Document it as "for inspection, not modification".

## Issue 5: Review error messages

### Current precondition

```clojure
{:pre [(vector? classes)]}
```

Produces: `Assert failed: (vector? classes)`

### Improvement option

More descriptive assertion:

```clojure
(assert (vector? classes)
        (str "resolve requires a vector of strings, got " (pr-str (type classes))
             ". Use (comp resolve normalize) for flexible input."))
```

### Decision

Keep `:pre` - it's idiomatic. The `normalize` function provides the escape hatch.
Document the requirement clearly in the docstring.

## Checklist

- [ ] Make `stroke-width?` private in config.cljc
- [ ] Remove empty `border-width-keywords` set from classify.cljc
- [ ] Simplify `border-width?` function
- [ ] Add docstring to `sort-modifiers` in api.cljc
- [ ] Verify all public functions have docstrings
- [ ] Run full test suite

## Verification

```sh
just          # Full test suite
just hygiene  # No new warnings
just lint     # clj-kondo clean
```

## Post-Cleanup Checks

After all phases complete:

1. **API surface**: Only `resolve`, `make-resolver`, `supported-patterns`, `normalize`
2. **No warnings**: `just hygiene` passes
3. **All tests pass**: `just` completes successfully
4. **Benchmarks stable**: Compare to baseline
5. **Documentation**: README reflects current API
