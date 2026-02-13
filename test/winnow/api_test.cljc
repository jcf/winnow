(ns winnow.api-test
  (:refer-clojure :exclude [resolve])
  (:require
   #?(:clj  [clojure.test :refer [deftest is]]
      :cljs [cljs.test :refer-macros [deftest is]])
   [winnow.api :as sut]))

(deftest resolve
  (is (= "" (sut/resolve []))))

(deftest make-resolver
  (let [resolve (sut/make-resolver {:colors #{"surface" "primary" "accent"}})]
    (is (= "bg-surface" (resolve ["bg-red bg-surface"])))
    (is (= "text-primary" (resolve ["text-black text-primary"])))
    (is (= "border-accent" (resolve ["border-blue border-accent"])))
    (is (= "bg-blue" (resolve ["bg-red bg-blue"])) "standard colors still work")))

(deftest prefix-support
  (let [resolve (sut/make-resolver {:prefix "tw-"})]
    (is (= "tw-hidden" (resolve ["tw-block tw-hidden"])))
    (is (= "tw-px-4" (resolve ["tw-px-2 tw-px-4"])))
    (is (= "tw-text-blue-500" (resolve ["tw-text-red-500 tw-text-blue-500"])))
    (is (= "tw-hover:p-4" (resolve ["tw-hover:p-2 tw-hover:p-4"]))))

  ;; Non-prefixed classes pass through unchanged when prefix is configured
  (let [resolve (sut/make-resolver {:prefix "tw:"})]
    (is (= "block hidden" (resolve ["block hidden"]))
        "non-prefixed pass through unchanged")
    (is (= "p-3 p-2" (resolve ["p-3 p-2"]))
        "non-prefixed don't merge")
    (is (= "px-4 tw:px-6" (resolve ["tw:px-2 px-4 tw:px-6"]))
        "only prefixed classes merge"))

  (let [resolve (sut/make-resolver {:prefix "tw:" :colors #{"primary"}})]
    (is (= "tw:hidden" (resolve ["tw:block tw:hidden"])))
    (is (= "tw:bg-primary" (resolve ["tw:bg-red tw:bg-primary"])))))
