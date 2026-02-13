(ns winnow.compare
  "Compare tailwind-merge-clj against winnow's conformance suite."
  (:require
   [clojure.string :as str]
   [twmerge.core :as tw-merge]))

;;; ----------------------------------------------------------------------------
;;; Conformance parser (from conformance_test.clj)

(defn- parse-spec
  [content]
  (loop [lines (str/split-lines content)
         input nil
         cases []]
    (if (empty? lines)
      cases
      (let [[line & rest] lines]
        (cond
          (or (str/starts-with? line "#")
              (str/blank? line))
          (recur rest input cases)

          (str/starts-with? line "=>")
          (let [expected (str/trim (subs line 2))]
            (recur rest nil (conj cases {:input    input
                                         :expected expected})))

          :else
          (recur rest (str/trim line) cases))))))

;;; ----------------------------------------------------------------------------
;;; Runner

(defn run-comparison
  []
  (let [tests   (parse-spec (slurp "spec/winnow.txt"))
        results (for [{:keys [input expected]} tests]
                  (let [combined (str/replace input " | " " ")
                        actual   (tw-merge/tw-merge combined)]
                    {:input    input
                     :expected expected
                     :actual   actual
                     :pass?    (= expected actual)}))]
    {:total  (count results)
     :passed (count (filter :pass? results))
     :failed (remove :pass? results)}))

(defn -main
  [& _args]
  (println)
  (println "tailwind-merge-clj Conformance Results")
  (println "═══════════════════════════════════════════════════════════════════")
  (println)
  (let [{:keys [total passed failed]} (run-comparison)]
    (println (format "  Total:  %d" total))
    (println (format "  Passed: %d (%.1f%%)" passed (* 100.0 (/ passed total))))
    (println (format "  Failed: %d" (count failed)))
    (println)
    (when (seq failed)
      (println "Failed tests (first 20):")
      (println "─────────────────────────────────────────────────────────────────")
      (doseq [{:keys [input expected actual]} (take 20 failed)]
        (println)
        (println "  Input:    " input)
        (println "  Expected: " expected)
        (println "  Actual:   " actual))
      (println))))
