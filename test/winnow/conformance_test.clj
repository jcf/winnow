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
  ;; Without configuration, unknown colors fall to generic `:bg` group and still
  ;; conflict (last wins), but as `:bg` not `:bg-color`.
  (is (= "bg-primary"
         (api/resolve ["bg-brand bg-primary"]))
      "unknown values on same prefix still conflict")

  ;; The key difference: without config, custom colors don't conflict with KNOWN
  ;; colors because they're in different groups.
  (is (= "bg-red bg-brand"
         (api/resolve ["bg-red bg-brand"]))
      "unknown 'brand' doesn't override known 'red' - different groups")

  ;; With configuration, custom colors properly conflict with standard ones.
  (let [resolve (api/make-resolver {:colors #{"brand"}})]
    (is (= "bg-brand"
           (resolve ["bg-red bg-brand"]))
        "configured 'brand' overrides 'red' - same :bg-color group")))
