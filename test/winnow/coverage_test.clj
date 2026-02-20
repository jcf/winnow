(ns winnow.coverage-test
  (:require
   [clojure.test :refer [deftest is]]
   [winnow.coverage :as coverage]))

(deftest upstream-coverage
  (let [{:keys [total covered missing coverage]} (coverage/analyze)]
    (is (<= 0.99 coverage)
        (format "Coverage dropped below 99%%: %d/%d (%.1f%%)\nMissing: %s"
                covered total (* 100 coverage) (pr-str missing)))
    (is (= #{"@container"} missing)
        (format "Unexpected missing utilities: %s"
                (pr-str (disj missing "@container"))))))

(deftest upstream-data-available
  (let [utilities (coverage/load-upstream)]
    (is (set? utilities))
    (is (< 800 (count utilities)))))
