(ns winnow.test.spec
  (:require
   [clojure.pprint :refer [pprint]]
   [clojure.spec.alpha :as s]
   [clojure.spec.test.alpha :as stest]
   [clojure.test :as t]
   [clojure.test.check]))

(defmethod t/assert-expr 'valid?
  [msg form]
  (let [[_ spec val] form]
    `(let [result# (s/valid? ~spec ~val)]
       (if result#
         (t/do-report {:type     :pass
                       :expected '~form
                       :actual   '~form
                       :message  ~msg})
         (t/do-report {:type     :fail
                       :expected '~form
                       :actual   (::s/problems (s/explain-data ~spec ~val))
                       :message  ~msg})))))

(defmethod t/assert-expr 'invalid?
  [msg form]
  (let [[_ spec val] form]
    `(let [problems# (s/explain-data ~spec ~val)]
       (if (some? problems#)
         (t/do-report {:type     :pass
                       :expected '~form
                       :actual   '~form
                       :message  ~msg})
         (t/do-report {:type     :fail
                       :expected '~form
                       :actual   (str ~val " unexpectedly conformed to " ~spec)
                       :message  ~msg})))))

(defmethod t/assert-expr 'well-specified?
  [msg form]
  (let [ns (nth form 1)]
    `(doseq [result# (stest/check (stest/enumerate-namespace ~ns)
                                  {:clojure.spec.test.check/opts {:num-tests 100}})]
       (if (nil? (:failure result#))
         (t/do-report {:type     :pass
                       :expected '~form
                       :actual   (:sym result#)
                       :message  ~msg})
         (t/do-report {:type     :fail
                       :expected '~form
                       :actual   (with-out-str (pprint result#))
                       :message  ~msg})))))
