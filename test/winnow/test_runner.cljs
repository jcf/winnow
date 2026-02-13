(ns winnow.test-runner
  (:require
   [cljs.test :refer [run-tests]]
   [winnow.api-test]
   [winnow.parse-test]))

(defn -main []
  (run-tests 'winnow.api-test 'winnow.parse-test))

(-main)
