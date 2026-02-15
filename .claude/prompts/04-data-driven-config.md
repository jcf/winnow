# Phase 4: Data-Driven Configuration

## Goal

Move hardcoded relationships into configuration maps for extensibility.

## Issue: Hardcoded postfix-conflicts

### Current

```clojure
;; api.cljc:84-87
(defn- postfix-conflicts
  [group postfix]
  (when (and postfix (= group :text-size))
    [:leading]))
```

This hardcodes the `text-size`/`leading` relationship. When `text-xl/8` is used,
the `/8` sets line-height, so it conflicts with `leading-*` utilities.

### Proposed

Add to `config/default`:

```clojure
:postfix-conflicts
{:text-size [:leading]}
```

Update `api.cljc`:

```clojure
(defn- postfix-conflicts
  [config group postfix]
  (when postfix
    (get-in config [:postfix-conflicts group])))
```

Update call site in `process-class`:

```clojure
;; Before
:extra-conflicts (postfix-conflicts group (:postfix-at parsed))

;; After
:extra-conflicts (postfix-conflicts config group (:postfix-at parsed))
```

## Implementation

### Step 1: Update config.cljc

Add to the `default` map after `:conflicts`:

```clojure
:postfix-conflicts
{:text-size [:leading]}
```

### Step 2: Update api.cljc

Change the function signature:

```clojure
(defn- postfix-conflicts
  [config group postfix]
  (when postfix
    (get-in config [:postfix-conflicts group])))
```

Update `process-class` to pass config:

```clojure
(defn- process-class
  [config raw]
  (let [[has-prefix? unprefixed] (strip-prefix (:class-prefix config) raw)]
    (when has-prefix?
      (let [parsed (parse/parse-class unprefixed)
            group  (classify/classify config (:base parsed))]
        (when group
          (let [mods       (sort-modifiers (:modifiers parsed)
                                           (:order-sensitive-modifiers config))
                mod-prefix (cond-> (str/join ":" mods)
                             (:important? parsed) (str "!"))]
            {:raw              raw
             :group            group
             :id               (make-id mod-prefix group)
             :mod-prefix       mod-prefix
             :extra-conflicts  (postfix-conflicts config group (:postfix-at parsed))}))))))
```

### Step 3: Add test coverage

Add to conformance tests or api_test.cljc:

```clojure
(deftest postfix-conflicts
  (testing "text size with line-height postfix conflicts with leading"
    (is (= "text-xl/8" (sut/resolve ["leading-6" "text-xl/8"]))))

  (testing "text size without postfix does not conflict with leading"
    (is (= "leading-6 text-xl" (sut/resolve ["leading-6" "text-xl"])))))
```

## Future Extensibility

With this data-driven approach, users could potentially customize:

```clojure
(def resolve
  (api/make-resolver
   {:postfix-conflicts {:text-size [:leading :tracking]}}))
```

Not implementing this for 1.0, but the structure supports it.

## Verification

```sh
just          # Full test suite
just bench    # No performance regression
just hygiene  # No new warnings
```

## Notes

- This is a small change with low risk
- Makes the code self-documenting (config as data)
- Enables future customization without code changes
