(ns winnow.bench
  (:require
   [criterium.core :as crit]
   [winnow.api :as api]))

;;; ----------------------------------------------------------------------------
;;; Test data

(def benchmarks
  [{:name   "Small"
    :desc   "2 classes, 1 conflict"
    :input  ["text-black text-white"]}

   {:name   "Medium"
    :desc   "10 classes, 2 conflicts"
    :input  ["p-2 m-4 text-lg"
             "hover:bg-blue-500 focus:ring-2"
             "p-4 text-xl text-white"]}

   {:name   "Large"
    :desc   "25 classes, 8 conflicts"
    :input  ["flex items-center justify-between p-4 m-2"
             "bg-white dark:bg-gray-800 rounded-lg shadow-md"
             "hover:shadow-lg transition-shadow duration-200"
             "border border-gray-200 dark:border-gray-700"
             "p-6 m-4 bg-gray-100 rounded-xl"]}

   {:name   "Modifiers"
    :desc   "Complex modifier chains"
    :input  ["hover:focus:dark:lg:p-4"
             "[&>*]:underline [&>*]:line-through"
             "hover:focus:dark:lg:p-6"
             "group-hover:peer-focus:text-red-500"]}

   {:name   "Arbitrary"
    :desc   "Arbitrary values + type labels"
    :input  ["m-[2px] m-[10px]"
             "text-[length:var(--size)] text-[color:red]"
             "bg-[url(.)] bg-linear-to-r"
             "font-[weight:var(--a)] font-[400]"]}

   {:name   "No conflicts"
    :desc   "All classes preserved"
    :input  ["flex items-center gap-4"
             "text-lg font-medium text-gray-900"
             "rounded-lg border shadow-sm"]}])

;;; ----------------------------------------------------------------------------
;;; Spinner

(def spinner-frames ["⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏"])

(defn with-spinner
  [label f]
  (let [running (volatile! true)
        thread  (doto (Thread.
                        (fn []
                          (loop [i 0]
                            (when @running
                              (print (format "\r  %s %-14s"
                                             (nth spinner-frames (mod i 10))
                                             label))
                              (flush)
                              (Thread/sleep 80)
                              (recur (inc i))))))
                  (.setDaemon true)
                  (.start))]
    (try
      (f)
      (finally
        (vreset! running false)
        (.join thread 100)))))

;;; ----------------------------------------------------------------------------
;;; Formatting

(defn format-time
  [nanos]
  (cond
    (< nanos 1000)       (format "%6.0f ns" nanos)
    (< nanos 1000000)    (format "%6.2f µs" (/ nanos 1000.0))
    (< nanos 1000000000) (format "%6.2f ms" (/ nanos 1000000.0))
    :else                (format "%6.2f s"  (/ nanos 1000000000.0))))

(defn format-row
  [{:keys [name mean stddev]}]
  (format "  %-14s %s  ± %s" name (format-time mean) (format-time stddev)))

;;; ----------------------------------------------------------------------------
;;; Benchmark runner

(defn run-bench
  [{:keys [name input] :as bench}]
  (let [result (with-spinner name
                 #(crit/quick-benchmark* (fn [] (api/resolve input)) {}))
        mean   (first (:mean result))
        stddev (first (:variance result))]
    (println (format "\r  ✓ %-14s%s  ± %s"
                     name
                     (format-time (* mean 1e9))
                     (format-time (* (Math/sqrt stddev) 1e9))))
    (assoc bench
           :mean   (* mean 1e9)
           :stddev (* (Math/sqrt stddev) 1e9))))

(defn run-all
  []
  (println)
  (println "Winnow Resolve Benchmarks")
  (println "─────────────────────────────────────────")
  (println)
  (let [results (mapv run-bench benchmarks)]
    (println)
    (println "─────────────────────────────────────────")
    (let [slowest (apply max (map :mean results))
          fastest (apply min (map :mean results))]
      (println (format "  Range: %s – %s"
                       (format-time fastest)
                       (format-time slowest))))
    (println)))

(defn -main
  [& _args]
  (run-all))

(comment
  (run-all)
  (run-bench (first benchmarks))
  )
