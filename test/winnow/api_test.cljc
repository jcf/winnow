(ns winnow.api-test
  (:refer-clojure :exclude [resolve])
  (:require
   #?(:clj  [clojure.test :refer [are deftest is]]
      :cljs [cljs.test :refer-macros [are deftest is]])
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

(deftest postfix-conflicts
  (is (= "text-xl/8" (sut/resolve ["leading-6" "text-xl/8"]))
      "text size with line-height postfix conflicts with leading")
  (is (= "leading-6 text-xl" (sut/resolve ["leading-6" "text-xl"]))
      "text size without postfix does not conflict with leading"))

(deftest normalize
  (are [in out] (= out (sut/normalize in))
    nil                     []
    "p-4"                   ["p-4"]
    "p-4 m-2"               ["p-4 m-2"]
    ["p-4" "m-2"]           ["p-4" "m-2"]
    ["p-4" nil "m-2"]       ["p-4" "m-2"]
    [["a"] "b" ["c"]]       ["a" "b" "c"]
    [["a" nil] nil "b"]     ["a" "b"]
    '("a" "b")              ["a" "b"]
    [[["a"]]]               ["a"]
    [nil nil nil]           [])
  (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
               (sut/normalize 42)))
  (is (thrown? #?(:clj clojure.lang.ExceptionInfo :cljs js/Error)
               (sut/normalize {:class "p-4"}))))

(deftest normalize-with-resolve
  (let [tw (comp sut/resolve sut/normalize)]
    (are [in out] (= out (tw in))
      nil                     ""
      "p-4"                   "p-4"
      ["p-2" nil "p-4"]       "p-4"
      [["p-4"] "m-2"]         "p-4 m-2")))
