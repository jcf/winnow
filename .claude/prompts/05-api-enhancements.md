# Phase 5: API Enhancements

## Goal

Add `normalize` function for composable input handling without compromising
`resolve`'s strictness.

## Design Decision

**Keep `resolve` strict** - requires a vector, fails fast on bad input.

**Add `normalize`** - pure function that coerces inputs into vector format.
Users compose as needed: `(comp resolve normalize)`.

## Implementation

### Add to api.cljc

```clojure
(defn normalize
  "Normalizes input for resolve.

  Handles:
    nil                  -> []
    \"string\"           -> [\"string\"]
    [\"a\" nil \"b\"]    -> [\"a\" \"b\"]
    [[\"a\"] \"b\" nil]  -> [\"a\" \"b\"]
    (list \"a\" \"b\")   -> [\"a\" \"b\"]

  Returns a vector of strings suitable for resolve. Does not validate that
  strings contain valid Tailwind classes.

  Example:
    (def my-resolve (comp resolve normalize))
    (my-resolve nil)              ;; => \"\"
    (my-resolve \"p-4 m-2\")      ;; => \"p-4 m-2\"
    (my-resolve [\"p-4\" nil])    ;; => \"p-4\""
  [input]
  (cond
    (nil? input)
    []

    (string? input)
    [input]

    (sequential? input)
    (into []
          (comp (filter some?)
                (mapcat #(if (sequential? %) % [%]))
                (filter string?))
          input)

    :else
    (throw (ex-info "Cannot normalize input for resolve"
                    {:input input
                     :type  (type input)}))))
```

### Update namespace docstring

```clojure
(ns winnow.api
  "Tailwind CSS class merging for Clojure.

  This library resolves conflicting Tailwind CSS classes by keeping the last
  value for each conflict group. For example, `px-2 px-4` resolves to `px-4`.

  Basic usage:
    (require '[winnow.api :as winnow])
    (winnow/resolve [\"px-2 py-4\" \"px-6\"]) ;; => \"py-4 px-6\"

  Flexible input handling:
    (def my-resolve (comp winnow/resolve winnow/normalize))
    (my-resolve nil)              ;; => \"\"
    (my-resolve \"p-4 m-2\")      ;; => \"p-4 m-2\"
    (my-resolve [\"p-4\" nil])    ;; => \"p-4\"

  Custom colors:
    (def resolve (winnow/make-resolver {:colors #{\"primary\" \"surface\"}}))
    (resolve [\"bg-red-500 bg-primary\"]) ;; => \"bg-primary\"
  ..."
  (:refer-clojure :exclude [resolve])
  ...)
```

### Add tests

Add to `api_test.cljc`:

```clojure
(deftest normalize-test
  (testing "nil"
    (is (= [] (sut/normalize nil))))

  (testing "string"
    (is (= ["p-4"] (sut/normalize "p-4")))
    (is (= ["p-4 m-2"] (sut/normalize "p-4 m-2"))))

  (testing "vector"
    (is (= ["p-4" "m-2"] (sut/normalize ["p-4" "m-2"]))))

  (testing "vector with nils"
    (is (= ["p-4" "m-2"] (sut/normalize ["p-4" nil "m-2"]))))

  (testing "nested sequences"
    (is (= ["a" "b" "c"] (sut/normalize [["a"] "b" ["c"]])))
    (is (= ["a" "b"] (sut/normalize [["a" nil] nil "b"]))))

  (testing "list"
    (is (= ["a" "b"] (sut/normalize '("a" "b")))))

  (testing "filters non-strings from nested"
    (is (= ["a" "b"] (sut/normalize ["a" [42] "b"]))))

  (testing "throws on invalid input"
    (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                 (sut/normalize 42)))
    (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
                 (sut/normalize {:class "p-4"})))))

(deftest compose-resolve-normalize
  (let [resolve-lax (comp sut/resolve sut/normalize)]
    (is (= "" (resolve-lax nil)))
    (is (= "p-4" (resolve-lax "p-4")))
    (is (= "p-4" (resolve-lax ["p-2" nil "p-4"])))
    (is (= "p-4 m-2" (resolve-lax [["p-4"] "m-2"])))))
```

## Usage Patterns

### Simple composition

```clojure
(require '[winnow.api :as winnow])

(def tw (comp winnow/resolve winnow/normalize))

(tw nil)                    ;; => ""
(tw "p-4 m-2")              ;; => "p-4 m-2"
(tw ["base" nil "override"]) ;; => "base override" (with conflict resolution)
```

### In React/Reagent components

```clojure
(def tw (comp winnow/resolve winnow/normalize))

(defn button [{:keys [class disabled?]}]
  [:button {:class (tw ["px-4 py-2 rounded"
                        "bg-blue-500 text-white"
                        (when disabled? "opacity-50 cursor-not-allowed")
                        class])}])
```

### With custom resolver

```clojure
(def my-resolve
  (let [resolver (winnow/make-resolver {:colors #{"primary" "surface"}})]
    (comp resolver winnow/normalize)))

(my-resolve ["bg-red" "bg-primary"]) ;; => "bg-primary"
```

## Verification

```sh
just          # Full test suite
just test-bb  # Babashka compatibility
just test-cljs # ClojureScript compatibility
just hygiene  # No warnings
```

## Notes

- `normalize` flattens one level only - not recursive
- Non-string values in nested sequences are filtered out silently
- This matches common patterns like `[base (when cond class) override]`
- The error for invalid input includes the type for debugging
