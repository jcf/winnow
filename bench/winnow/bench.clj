(ns winnow.bench
  (:require
   [clojure.java.shell :as shell]
   [clojure.pprint :as pp]
   [clojure.string :as str]
   [criterium.core :as crit]
   [winnow.api :as api]))

;;; ----------------------------------------------------------------------------
;;; Metadata

(defn- git-commit []
  (-> (shell/sh "git" "rev-parse" "--short" "HEAD")
      :out
      str/trim))

(defn- jvm-version []
  (System/getProperty "java.version"))

(defn- os-info []
  (str (System/getProperty "os.name") " " (System/getProperty "os.version")))

(defn- timestamp []
  (.toString (java.time.Instant/now)))

(defn- build-meta []
  {:commit    (git-commit)
   :timestamp (timestamp)
   :jvm       (jvm-version)
   :os        (os-info)
   :clojure   (clojure-version)})

;;; ----------------------------------------------------------------------------
;;; Test data

(def benchmarks
  [{:name  "Small"
    :desc  "2 classes, 1 conflict"
    :input ["text-black text-white"]}

   {:name  "Medium"
    :desc  "10 classes, 2 conflicts"
    :input ["p-2 m-4 text-lg"
            "hover:bg-blue-500 focus:ring-2"
            "p-4 text-xl text-white"]}

   {:name  "Large"
    :desc  "25 classes, 8 conflicts"
    :input ["flex items-center justify-between p-4 m-2"
            "bg-white dark:bg-gray-800 rounded-lg shadow-md"
            "hover:shadow-lg transition-shadow duration-200"
            "border border-gray-200 dark:border-gray-700"
            "p-6 m-4 bg-gray-100 rounded-xl"]}

   {:name  "Modifiers"
    :desc  "Complex modifier chains"
    :input ["hover:focus:dark:lg:p-4"
            "[&>*]:underline [&>*]:line-through"
            "hover:focus:dark:lg:p-6"
            "group-hover:peer-focus:text-red-500"]}

   {:name  "Arbitrary"
    :desc  "Arbitrary values + type labels"
    :input ["m-[2px] m-[10px]"
            "text-[length:var(--size)] text-[color:red]"
            "bg-[url(.)] bg-linear-to-r"
            "font-[weight:var(--a)] font-[400]"]}

   {:name  "No-conflicts"
    :desc  "All classes preserved"
    :input ["flex items-center gap-4"
            "text-lg font-medium text-gray-900"
            "rounded-lg border shadow-sm"]}])

;;; ----------------------------------------------------------------------------
;;; Spinner (for pretty output)

(def ^:private spinner-frames ["⠋" "⠙" "⠹" "⠸" "⠼" "⠴" "⠦" "⠧" "⠇" "⠏"])

(defn- with-spinner
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
;;; Formatting (for pretty output)

(defn- format-time
  [seconds]
  (let [nanos (* seconds 1e9)]
    (cond
      (< nanos 1000)       (format "%6.0f ns" nanos)
      (< nanos 1000000)    (format "%6.2f µs" (/ nanos 1000.0))
      (< nanos 1000000000) (format "%6.2f ms" (/ nanos 1000000.0))
      :else                (format "%6.2f s"  (/ nanos 1000000000.0)))))

;;; ----------------------------------------------------------------------------
;;; Benchmark runner

(defn- run-single
  [{:keys [name desc input]} {:keys [pretty]}]
  (let [run-fn  #(crit/quick-benchmark* (fn [] (api/resolve input)) {})
        result  (if pretty
                  (with-spinner name run-fn)
                  (run-fn))
        mean    (first (:mean result))
        var     (first (:variance result))
        std-dev (Math/sqrt var)]
    (when pretty
      (println (format "\r  ✓ %-14s%s  ± %s"
                       name
                       (format-time mean)
                       (format-time std-dev))))
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

(defn- run-all
  [{:keys [pretty] :as opts}]
  (when pretty
    (println)
    (println "Winnow Benchmarks")
    (println "─────────────────────────────────────────"))
  (let [meta    (build-meta)
        _       (when pretty
                  (println (format "  %s | Clojure %s | JVM %s"
                                   (:commit meta)
                                   (:clojure meta)
                                   (:jvm meta)))
                  (println "─────────────────────────────────────────")
                  (println))
        results (mapv #(run-single % opts) benchmarks)
        data    {:meta meta :results results}]
    (when pretty
      (println)
      (println "─────────────────────────────────────────")
      (let [slowest (apply max (map :mean results))
            fastest (apply min (map :mean results))]
        (println (format "  Range: %s – %s"
                         (format-time fastest)
                         (format-time slowest))))
      (println))
    data))

(defn run
  ([] (run {}))
  ([{:keys [pretty] :as opts}]
   (let [data (run-all opts)]
     (when-not pretty
       (pp/pprint data))
     data)))

(defn -main [& args]
  (let [pretty (boolean (some #{"--pretty" "-p"} args))]
    (run {:pretty pretty})
    (shutdown-agents)))

(comment
  (run {:pretty true})
  (run)
  (run-single (first benchmarks) {})
  )
