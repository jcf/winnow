(ns winnow.conformance-test
  (:refer-clojure :exclude [resolve])
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is]]
   [winnow.api :as api]))

;;; ----------------------------------------------------------------------------
;;; Test colors used in conformance spec
;;;
;;; The conformance spec uses made-up color names to test conflict resolution.
;;; These must be configured for the resolver to recognize them.

(def ^:private colors
  #{"grey" "hotpink" "some" "other" "coloooor"})

(def ^:private resolve
  (api/make-resolver {:colors colors}))

;;; ----------------------------------------------------------------------------
;;; Spec parsing

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
;;; Tests

(deftest conformance
  (doseq [{:keys [input expected]} (parse-spec (slurp "spec/winnow.txt"))]
    (let [parts (str/split input #" \| ")]
      (is (= expected (resolve parts)) input))))

(deftest custom-colors-require-configuration
  ;; Unknown values pass through unchanged without participating in conflict
  ;; resolution. They don't conflict with known utilities OR with each other.
  (is (= "bg-brand bg-primary"
         (api/resolve ["bg-brand bg-primary"]))
      "unknown values pass through unchanged")

  ;; Unknown values don't conflict with known utilities
  (is (= "bg-red bg-brand"
         (api/resolve ["bg-red bg-brand"]))
      "unknown 'brand' doesn't conflict with known 'red'")

  ;; With configuration, custom colors properly conflict with standard ones.
  (let [resolve (api/make-resolver {:colors #{"brand"}})]
    (is (= "bg-brand"
           (resolve ["bg-red bg-brand"]))
        "configured 'brand' overrides 'red' - same :bg-color group")))
