# Phase 3: Optimize merge-classes

## Goal

Refactor `merge-classes` to be more idiomatic using `reduce` with `rseq`.

## Current Implementation

```clojure
(defn- merge-classes
  [config classes]
  (let [processed (into []
                        (comp (mapcat parse/split-classes)
                              (map #(or (process-class config %)
                                        {:raw % :group nil})))
                        classes)]
    (loop [i      (dec (count processed))
           seen   #{}
           result '()]
      (if (neg? i)
        result
        (let [{:keys [raw group id mod-prefix extra-conflicts]} (nth processed i)]
          (if (nil? group)
            (recur (dec i) seen (conj result raw))
            (if (seen id)
              (recur (dec i) seen result)
              (let [conflicts (into (get-in config [:conflicts group] [])
                                    extra-conflicts)
                    new-seen  (into (conj seen id)
                                    (map #(make-id mod-prefix %))
                                    conflicts)]
                (recur (dec i) new-seen (conj result raw))))))))))
```

## Issues

1. Explicit index management with `(dec i)` and `(nth processed i)`
2. Nested `if` statements reduce readability
3. `into` creates intermediate vector for conflicts

## Proposed Implementation

```clojure
(defn- merge-classes
  [config classes]
  (let [processed (into []
                        (comp (mapcat parse/split-classes)
                              (map #(or (process-class config %)
                                        {:raw % :group nil})))
                        classes)]
    (first
     (reduce
      (fn [[result seen] {:keys [raw group id mod-prefix extra-conflicts]}]
        (cond
          ;; Unknown class - pass through
          (nil? group)
          [(conj result raw) seen]

          ;; Already seen this conflict group - skip
          (seen id)
          [result seen]

          ;; New class - add to result and mark conflicts as seen
          :else
          (let [all-conflicts (concat (get-in config [:conflicts group])
                                      extra-conflicts)
                new-seen      (into (conj seen id)
                                    (map #(make-id mod-prefix %))
                                    all-conflicts)]
            [(conj result raw) new-seen])))
      ['() #{}]
      (rseq processed)))))
```

## Changes

| Before               | After         | Reason                |
| -------------------- | ------------- | --------------------- |
| `loop` with index    | `reduce`      | More idiomatic        |
| `(nth processed i)`  | destructuring | Clearer               |
| Nested `if`          | `cond`        | Readable              |
| `(into [...] extra)` | `concat`      | Lazy, no intermediate |
| Manual `(dec i)`     | `rseq`        | Built-in reverse      |

## Alternative: Keep Loop

If benchmarks show the loop is faster, keep it with improved readability:

```clojure
(defn- merge-classes
  [config classes]
  (let [processed (into []
                        (comp (mapcat parse/split-classes)
                              (map #(or (process-class config %)
                                        {:raw % :group nil})))
                        classes)]
    (loop [i      (dec (count processed))
           seen   #{}
           result '()]
      (if (neg? i)
        result
        (let [{:keys [raw group id mod-prefix extra-conflicts]} (nth processed i)]
          (cond
            (nil? group)
            (recur (dec i) seen (conj result raw))

            (seen id)
            (recur (dec i) seen result)

            :else
            (let [all-conflicts (concat (get-in config [:conflicts group])
                                        extra-conflicts)
                  new-seen      (into (conj seen id)
                                      (map #(make-id mod-prefix %))
                                      all-conflicts)]
              (recur (dec i) new-seen (conj result raw)))))))))
```

## Benchmarking Strategy

1. Capture baseline before change
2. Implement `reduce` version
3. Benchmark
4. If slower, try loop-with-cond version
5. Pick faster option

```sh
# Before
just bench > bench/before-merge.edn

# After reduce version
just bench > bench/after-merge-reduce.edn

# Compare
just bench-compare bench/before-merge.edn bench/after-merge-reduce.edn
```

Focus on "Large" benchmark (25 classes, 8 conflicts) - most relevant for merge.

## Verification

```sh
just          # Full test suite - correctness first
just bench    # Performance comparison
just hygiene  # No new warnings
```

## Notes

- `rseq` on a vector is O(1) - returns a reverse seq without copying
- `conj` on a list prepends in O(1)
- The combination of `rseq` + list `conj` maintains correct order
- `concat` is lazy - avoids intermediate collection for conflicts
