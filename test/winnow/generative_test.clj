(ns winnow.generative-test
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :refer [for-all]]
   [winnow.api :as api]
   [winnow.generators :as g]))

(def ^:private num-tests 100)

;;; ----------------------------------------------------------------------------
;;; Derived generators

(def gen-display
  (gen/elements ["block" "inline-block" "inline" "flex" "grid" "hidden"]))

(def gen-position
  (gen/elements ["static" "fixed" "absolute" "relative" "sticky"]))

(def gen-non-conflicting
  (gen/one-of
   [(gen/fmap (fn [[v1 v2]] [(str "px-" v1) (str "py-" v2)])
              (gen/tuple g/gen-spacing-value g/gen-spacing-value))
    (gen/fmap (fn [[c s]] [(str "text-" c) (str "text-" s)])
              (gen/tuple g/gen-color-value
                         (gen/elements ["xs" "sm" "base" "lg" "xl"])))
    (gen/tuple gen-display gen-position)]))

(def gen-stroke-color-and-width
  (gen/fmap (fn [[color width]]
              [(str "stroke-" color) (str "stroke-" width)])
            (gen/tuple g/gen-color-value g/gen-arbitrary-length)))

(def gen-outline-color-and-width
  (gen/fmap (fn [[color width]]
              [(str "outline-" color) (str "outline-" (inc width))])
            (gen/tuple g/gen-color-value (gen/choose 1 8))))

(def gen-important-class
  (gen/let [base g/gen-base-class
            style (gen/elements [:prefix :suffix])]
    (case style
      :prefix (str "!" base)
      :suffix (str base "!"))))

(def gen-content-value
  (gen/elements ["['hello']" "[attr(data-content)]" "[var(--content)]" "['']"]))

(def gen-conflicting-content
  (gen/fmap (fn [[v1 v2]] [(str "content-" v1) (str "content-" v2)])
            (gen/tuple gen-content-value gen-content-value)))

(def gen-inset-hierarchy
  (gen/let [v1 g/gen-spacing-value
            v2 g/gen-spacing-value]
    (gen/elements
     [[(str "inset-x-" v1) (str "inset-" v2)]
      [(str "inset-y-" v1) (str "inset-" v2)]
      [(str "right-" v1) (str "inset-x-" v2)]
      [(str "left-" v1) (str "inset-x-" v2)]])))

;;; ----------------------------------------------------------------------------
;;; Helpers

(defn- resolve-classes
  [classes]
  (api/resolve [(str/join " " classes)]))

(defn- result-classes
  [s]
  (if (str/blank? s)
    #{}
    (set (str/split s #"\s+"))))

;;; ----------------------------------------------------------------------------
;;; Properties

(defspec idempotence num-tests
  (for-all [classes g/gen-tailwind-class-list]
    (let [once  (resolve-classes classes)
          twice (resolve-classes [once])]
      (= once twice))))

(defspec output-is-subset-of-input num-tests
  (for-all [classes g/gen-tailwind-class-list]
    (let [result (result-classes (resolve-classes classes))
          input  (set classes)]
      (set/subset? result input))))

(defspec last-color-wins num-tests
  (for-all [[c1 c2] g/gen-conflicting-colors]
    (str/includes? (resolve-classes [c1 c2]) c2)))

(defspec last-spacing-wins num-tests
  (for-all [[c1 c2] g/gen-conflicting-spacing]
    (str/includes? (resolve-classes [c1 c2]) c2)))

(defspec last-display-wins num-tests
  (for-all [d1 gen-display
            d2 gen-display]
    (= d2 (resolve-classes [d1 d2]))))

(defspec non-conflicting-preserved num-tests
  (for-all [[c1 c2] gen-non-conflicting]
    (let [result (result-classes (resolve-classes [c1 c2]))]
      (and (contains? result c1)
           (contains? result c2)))))

(defspec modifiers-separate-groups num-tests
  (for-all [base g/gen-base-class
            mod  g/gen-modifier]
    (let [modified (str mod ":" base)
          result   (resolve-classes [base modified])]
      (and (str/includes? result base)
           (str/includes? result modified)))))

(defspec empty-yields-empty num-tests
  (for-all [_ gen/nat]
    (= "" (api/resolve []))))

(defspec negative-positive-conflict num-tests
  (for-all [prefix (gen/elements ["m" "mx" "my" "mt" "mr" "mb" "ml"
                                  "top" "right" "bottom" "left"])
            v1     g/gen-spacing-value
            v2     g/gen-spacing-value]
    (let [neg (str "-" prefix "-" v1)
          pos (str prefix "-" v2)]
      (= pos (resolve-classes [neg pos])))))

(defspec stroke-color-and-width-coexist num-tests
  (for-all [[color width] gen-stroke-color-and-width]
    (let [result (result-classes (resolve-classes [color width]))]
      (and (contains? result color)
           (contains? result width)))))

(defspec outline-color-and-width-coexist num-tests
  (for-all [[color width] gen-outline-color-and-width]
    (let [result (result-classes (resolve-classes [color width]))]
      (and (contains? result color)
           (contains? result width)))))

(defspec content-utilities-conflict num-tests
  (for-all [[c1 c2] gen-conflicting-content]
    (str/includes? (resolve-classes [c1 c2]) c2)))

(defspec important-modifier-preserved num-tests
  (for-all [important gen-important-class]
    (let [result (resolve-classes [important])]
      (or (str/includes? result "!")
          (str/includes? result important)))))

(defspec inset-hierarchy-respected num-tests
  (for-all [[specific general] gen-inset-hierarchy]
    (let [result (resolve-classes [specific general])]
      (str/includes? result general))))
