(ns winnow.spec-test
  (:require
   [clojure.test :refer [deftest is]]
   [winnow.api :as api]
   [winnow.config :as config]
   [winnow.spec]
   [winnow.test.spec]))

(deftest data-specs
  (is (valid? ::api/classes []))
  (is (valid? ::api/classes ["px-2" "py-4 mx-auto"]))
  (is (invalid? ::api/classes "not a vector"))
  (is (invalid? ::api/classes [1 2 3]))

  (is (valid? ::api/resolver-config {}))
  (is (valid? ::api/resolver-config {:prefix "tw-" :colors #{"primary"}}))

  (is (valid? ::api/supported-patterns (api/supported-patterns)))

  (is (valid? ::config/colors #{"red" "blue"}))
  (is (invalid? ::config/colors ["red" "blue"])))

(deftest check-api
  (is (well-specified? 'winnow.api)))

(deftest instrument!
  (is (= '[winnow.api/resolve
           winnow.api/make-resolver
           winnow.api/supported-patterns]
         (winnow.spec/instrument!))))
