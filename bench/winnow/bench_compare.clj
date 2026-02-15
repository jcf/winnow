(ns winnow.bench-compare
  (:require
   [clojure.edn :as edn]))

;;; ----------------------------------------------------------------------------
;;; Formatting

(defn- format-time
  [seconds]
  (let [nanos (* seconds 1e9)]
    (cond
      (< nanos 1000)       (format "%7.1f ns" nanos)
      (< nanos 1000000)    (format "%7.2f µs" (/ nanos 1000.0))
      (< nanos 1000000000) (format "%7.2f ms" (/ nanos 1000000.0))
      :else                (format "%7.2f s"  (/ nanos 1000000000.0)))))

(defn- delta-pct
  [before after]
  (if (zero? before)
    0.0
    (* 100.0 (/ (- after before) before))))

(defn- format-delta
  [pct]
  (let [sign   (cond (pos? pct) "+" (neg? pct) "" :else " ")
        color  (cond
                 (< pct -5)  "\u001b[32m"  ; green - significant improvement
                 (> pct 5)   "\u001b[31m"  ; red - significant regression
                 :else       "")
        reset  (if (seq color) "\u001b[0m" "")]
    (format "%s%s%6.1f%%%s" color sign pct reset)))

;;; ----------------------------------------------------------------------------
;;; Comparison

(defn- load-results
  [path]
  (-> (slurp path) edn/read-string))

(defn- results-by-name
  [data]
  (into {} (map (juxt :name identity) (:results data))))

(defn compare-results
  [before-path after-path]
  (let [before (load-results before-path)
        after  (load-results after-path)
        b-map  (results-by-name before)
        a-map  (results-by-name after)
        names  (map :name (:results before))]
    (println)
    (println "Benchmark Comparison")
    (println "────────────────────────────────────────────────────────────")
    (println (format "  Before: %s (%s)"
                     (get-in before [:meta :commit])
                     before-path))
    (println (format "  After:  %s (%s)"
                     (get-in after [:meta :commit])
                     after-path))
    (println "────────────────────────────────────────────────────────────")
    (println (format "  %-14s %11s %11s %10s" "Name" "Before" "After" "Delta"))
    (println "────────────────────────────────────────────────────────────")
    (let [deltas (for [name names
                       :let [b-mean (:mean (b-map name))
                             a-mean (:mean (a-map name))
                             delta  (delta-pct b-mean a-mean)]]
                   (do
                     (println (format "  %-14s %11s %11s %s"
                                      name
                                      (format-time b-mean)
                                      (format-time a-mean)
                                      (format-delta delta)))
                     delta))
          avg-delta (/ (reduce + deltas) (count deltas))]
      (println "────────────────────────────────────────────────────────────")
      (println (format "  %-14s %11s %11s %s"
                       "Average"
                       ""
                       ""
                       (format-delta avg-delta)))
      (println)
      {:before-commit (get-in before [:meta :commit])
       :after-commit  (get-in after [:meta :commit])
       :avg-delta-pct avg-delta
       :deltas        (zipmap names deltas)})))

(defn -main
  [& args]
  (if (< (count args) 2)
    (do
      (println "Usage: clojure -M:bench -m winnow.bench-compare <before.edn> <after.edn>")
      (System/exit 1))
    (let [[before after] args]
      (compare-results before after)
      (shutdown-agents))))

(comment
  (compare-results "bench/baseline.edn" "bench/after.edn")
  )
