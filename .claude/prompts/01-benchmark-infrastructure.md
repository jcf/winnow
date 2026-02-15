# Phase 1: Benchmark Infrastructure

## Goal

Update benchmarks to output machine-readable EDN with full Criterium statistics.
This enables reliable before/after comparisons across refactoring phases.

## Current State

`bench/winnow/bench.clj` outputs human-readable text with spinners:

```
Winnow Resolve Benchmarks
─────────────────────────────────────────

  ✓ Small           1.23 µs  ± 0.05 µs
  ...
```

Discards most Criterium data (quartiles, outliers, sample count).

## Proposed Changes

### 1. EDN Output by Default

```clojure
{:meta
 {:commit    "99be3f9"
  :timestamp "2026-02-15T10:30:00Z"
  :jvm       "21.0.1"
  :os        "Mac OS X 15.3"
  :clojure   "1.12.4"}

 :results
 [{:name     "Small"
   :desc     "2 classes, 1 conflict"
   :input    ["text-black text-white"]
   :mean     1.23e-6
   :variance 4.5e-14
   :std-dev  2.1e-7
   :lower-q  1.1e-6
   :upper-q  1.4e-6
   :outliers {:low-severe 0 :low-mild 1 :high-mild 2 :high-severe 0}
   :samples  60}
  ...]}
```

### 2. Pretty Output via Flag

```sh
just bench              # EDN to stdout
just bench --pretty     # Human-readable table
```

### 3. Comparison Script

Add `bench/compare.clj` that reads two EDN files and reports:

```
Benchmark Comparison
────────────────────────────────────────────────────────────
                  baseline.edn    after.edn       delta
Small             1.23 µs         1.19 µs         -3.2%
Medium            4.56 µs         4.48 µs         -1.8%
Large            12.34 µs        11.89 µs         -3.6%
...
────────────────────────────────────────────────────────────
```

## Implementation

### bench/winnow/bench.clj

```clojure
(ns winnow.bench
  (:require
   [clojure.edn :as edn]
   [clojure.java.shell :as shell]
   [clojure.pprint :as pp]
   [criterium.core :as crit]
   [winnow.api :as api]))

;;; ----------------------------------------------------------------------------
;;; Metadata

(defn- git-commit []
  (-> (shell/sh "git" "rev-parse" "--short" "HEAD")
      :out
      clojure.string/trim))

(defn- jvm-version []
  (System/getProperty "java.version"))

(defn- os-info []
  (str (System/getProperty "os.name") " " (System/getProperty "os.version")))

(defn- clojure-version-str []
  (clojure-version))

(defn- timestamp []
  (.toString (java.time.Instant/now)))

(defn- build-meta []
  {:commit    (git-commit)
   :timestamp (timestamp)
   :jvm       (jvm-version)
   :os        (os-info)
   :clojure   (clojure-version-str)})

;;; ----------------------------------------------------------------------------
;;; Benchmarks

(def benchmarks
  [{:name  "Small"
    :desc  "2 classes, 1 conflict"
    :input ["text-black text-white"]}
   ;; ... rest unchanged
   ])

(defn- run-single [{:keys [name desc input]}]
  (let [result  (crit/quick-benchmark* #(api/resolve input) {})
        mean    (first (:mean result))
        var     (first (:variance result))
        std-dev (Math/sqrt var)]
    {:name     name
     :desc     desc
     :input    input
     :mean     mean
     :variance var
     :std-dev  std-dev
     :lower-q  (first (:lower-q result))
     :upper-q  (first (:upper-q result))
     :outliers (:outliers result)
     :samples  (count (:results result))}))

;;; ----------------------------------------------------------------------------
;;; Output formats

(defn- run-all-edn []
  {:meta    (build-meta)
   :results (mapv run-single benchmarks)})

(defn- format-time [seconds]
  (let [nanos (* seconds 1e9)]
    (cond
      (< nanos 1000)       (format "%6.0f ns" nanos)
      (< nanos 1000000)    (format "%6.2f µs" (/ nanos 1000.0))
      (< nanos 1000000000) (format "%6.2f ms" (/ nanos 1000000.0))
      :else                (format "%6.2f s"  (/ nanos 1000000000.0)))))

(defn- print-pretty [{:keys [meta results]}]
  (println)
  (println "Winnow Benchmarks")
  (println (format "commit: %s | %s | JVM %s"
                   (:commit meta) (:clojure meta) (:jvm meta)))
  (println "─────────────────────────────────────────")
  (println)
  (doseq [{:keys [name mean std-dev]} results]
    (println (format "  %-14s %s  ± %s"
                     name
                     (format-time mean)
                     (format-time std-dev))))
  (println)
  (println "─────────────────────────────────────────")
  (let [slowest (apply max (map :mean results))
        fastest (apply min (map :mean results))]
    (println (format "  Range: %s – %s"
                     (format-time fastest)
                     (format-time slowest))))
  (println))

;;; ----------------------------------------------------------------------------
;;; Entry points

(defn run
  ([] (run {}))
  ([{:keys [pretty]}]
   (let [data (run-all-edn)]
     (if pretty
       (print-pretty data)
       (pp/pprint data)))))

(defn -main [& args]
  (run {:pretty (some #{"--pretty" "-p"} args)}))
```

### bench/winnow/compare.clj

```clojure
(ns winnow.compare
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]))

(defn- load-results [path]
  (-> (slurp path) edn/read-string))

(defn- format-time [seconds]
  (let [nanos (* seconds 1e9)]
    (cond
      (< nanos 1000)       (format "%.0f ns" nanos)
      (< nanos 1000000)    (format "%.2f µs" (/ nanos 1000.0))
      :else                (format "%.2f ms" (/ nanos 1000000.0)))))

(defn- delta-pct [before after]
  (* 100.0 (/ (- after before) before)))

(defn compare-results [before-path after-path]
  (let [before  (load-results before-path)
        after   (load-results after-path)
        b-map   (into {} (map (juxt :name identity) (:results before)))
        a-map   (into {} (map (juxt :name identity) (:results after)))]
    (println)
    (println "Benchmark Comparison")
    (println (format "Before: %s (%s)"
                     (:commit (:meta before))
                     before-path))
    (println (format "After:  %s (%s)"
                     (:commit (:meta after))
                     after-path))
    (println "────────────────────────────────────────────────────────────")
    (println (format "  %-14s %12s %12s %10s" "Name" "Before" "After" "Delta"))
    (println "────────────────────────────────────────────────────────────")
    (doseq [name (map :name (:results before))]
      (let [b-mean (:mean (b-map name))
            a-mean (:mean (a-map name))
            delta  (delta-pct b-mean a-mean)]
        (println (format "  %-14s %12s %12s %+9.1f%%"
                         name
                         (format-time b-mean)
                         (format-time a-mean)
                         delta))))
    (println "────────────────────────────────────────────────────────────")
    (println)))

(defn -main [before after]
  (compare-results before after))
```

### justfile updates

```just
# Run benchmarks (EDN output)
bench:
    clojure -M:bench -m winnow.bench

# Run benchmarks (human-readable)
bench-pretty:
    clojure -M:bench -m winnow.bench --pretty

# Compare two benchmark files
bench-compare before after:
    clojure -M:bench -m winnow.compare {{before}} {{after}}

# Capture baseline
bench-baseline:
    clojure -M:bench -m winnow.bench > bench/baseline.edn
    @echo "Saved to bench/baseline.edn"
```

## Verification

```sh
just bench              # Should output EDN
just bench-pretty       # Should output table
just bench > bench/test1.edn
just bench > bench/test2.edn
just bench-compare bench/test1.edn bench/test2.edn
```

## Notes

- Criterium's `quick-benchmark*` runs fewer samples than `benchmark*`
- For final release comparisons, consider using `benchmark*` for more precision
- The comparison script assumes benchmark names match between files
