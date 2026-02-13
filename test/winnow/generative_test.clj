(ns winnow.generative-test
  (:require
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :refer [for-all]]
   [winnow.api :as api]
   [winnow.config :as config]))

(def ^:private num-tests 100)

;;; ----------------------------------------------------------------------------
;;; Generators

(def gen-color
  (gen/elements (vec (:colors config/default))))

(def gen-shade
  (gen/elements ["50" "100" "200" "300" "400" "500" "600" "700" "800" "900" "950"]))

(def gen-color-with-shade
  (gen/fmap (fn [[c s]] (str c "-" s))
            (gen/tuple gen-color gen-shade)))

(def gen-opacity
  (gen/elements ["/0" "/5" "/10" "/25" "/50" "/75" "/90" "/100"]))

(def gen-color-value
  (gen/one-of
   [gen-color
    gen-color-with-shade
    (gen/fmap (fn [[c o]] (str c o))
              (gen/tuple gen-color-with-shade gen-opacity))]))

(def gen-spacing
  (gen/elements ["0" "1" "2" "3" "4" "5" "6" "8" "10" "12" "16" "20" "24" "32"
                 "px" "0.5" "1.5" "2.5" "3.5" "auto" "full"]))

(def gen-css-unit
  (gen/elements ["px" "rem" "em" "%" "vh" "vw"]))

(def gen-arbitrary-length
  (gen/fmap (fn [[n u]] (str "[" n u "]"))
            (gen/tuple (gen/choose 1 999) gen-css-unit)))

(def gen-hex-color
  (gen/fmap (fn [[r g b]] (format "[#%02x%02x%02x]" r g b))
            (gen/tuple (gen/choose 0 255)
                       (gen/choose 0 255)
                       (gen/choose 0 255))))

(def gen-arbitrary-color
  "Arbitrary color values that should be recognized as colors."
  (gen/one-of
   [gen-hex-color
    (gen/fmap #(str "[rgb(" % ",0,0)]") (gen/choose 0 255))
    (gen/fmap #(str "[hsl(" % "_80%_50%)]") (gen/choose 0 360))]))

(def gen-arbitrary-width
  "Arbitrary width values that should NOT be recognized as colors."
  (gen/one-of
   [gen-arbitrary-length
    (gen/fmap #(str "[" % "]") (gen/choose 1 10))]))

(def gen-breakpoint
  (gen/elements ["sm" "md" "lg" "xl" "2xl"]))

(def gen-pseudo
  (gen/elements ["hover" "focus" "active" "disabled" "first" "last"
                 "focus-within" "focus-visible"]))

(def gen-modifier
  (gen/one-of [gen-pseudo
               gen-breakpoint
               (gen/return "dark")
               (gen/fmap #(str "group-" %) gen-pseudo)
               (gen/fmap #(str "peer-" %) gen-pseudo)]))

;;; ----------------------------------------------------------------------------
;;; Utility generators

(def gen-spacing-utility
  (gen/fmap (fn [[p v]] (str p "-" v))
            (gen/tuple (gen/elements ["p" "px" "py" "m" "mx" "my" "w" "h" "gap"])
                       (gen/one-of [gen-spacing gen-arbitrary-length]))))

(def gen-color-utility
  (gen/fmap (fn [[p c]] (str p "-" c))
            (gen/tuple (gen/elements ["text" "bg" "border" "ring" "shadow"])
                       (gen/one-of [gen-color-value gen-hex-color]))))

(def gen-display
  (gen/elements ["block" "inline-block" "inline" "flex" "grid" "hidden"]))

(def gen-position
  (gen/elements ["static" "fixed" "absolute" "relative" "sticky"]))

(def gen-base-class
  (gen/one-of [gen-spacing-utility
               gen-color-utility
               gen-display
               gen-position]))

(def gen-tailwind-class
  (gen/let [mods (gen/vector gen-modifier 0 2)
            base gen-base-class]
    (if (seq mods)
      (str (str/join ":" mods) ":" base)
      base)))

(def gen-tailwind-class-list
  (gen/vector gen-tailwind-class 1 8))

;;; ----------------------------------------------------------------------------
;;; Conflict generators

(def gen-conflicting-colors
  (gen/fmap (fn [[p c1 c2]] [(str p "-" c1) (str p "-" c2)])
            (gen/tuple (gen/elements ["text" "bg" "border"])
                       gen-color-value
                       gen-color-value)))

(def gen-conflicting-spacing
  (gen/fmap (fn [[p v1 v2]] [(str p "-" v1) (str p "-" v2)])
            (gen/tuple (gen/elements ["p" "m" "w" "h" "gap"])
                       gen-spacing
                       gen-spacing)))

(def gen-non-conflicting
  (gen/one-of
   [(gen/fmap (fn [[v1 v2]] [(str "px-" v1) (str "py-" v2)])
              (gen/tuple gen-spacing gen-spacing))
    (gen/fmap (fn [[c s]] [(str "text-" c) (str "text-" s)])
              (gen/tuple gen-color-value
                         (gen/elements ["xs" "sm" "base" "lg" "xl"])))
    (gen/tuple gen-display gen-position)]))

;;; ----------------------------------------------------------------------------
;;; Negative value generators

(def gen-negative-spacing
  (gen/fmap (fn [[p v]] (str "-" p "-" v))
            (gen/tuple (gen/elements ["m" "mx" "my" "mt" "mr" "mb" "ml"
                                      "top" "right" "bottom" "left"
                                      "inset" "inset-x" "inset-y"])
                       gen-spacing)))

;;; ----------------------------------------------------------------------------
;;; Stroke/outline color vs width (non-conflicting)

(def gen-stroke-color-and-width
  "Stroke color and width are different groups - should not conflict."
  (gen/fmap (fn [[color width]]
              [(str "stroke-" color) (str "stroke-" width)])
            (gen/tuple gen-color-value gen-arbitrary-width)))

(def gen-outline-color-and-width
  "Outline color and width are different groups - should not conflict."
  (gen/fmap (fn [[color width]]
              [(str "outline-" color) (str "outline-" (inc width))])
            (gen/tuple gen-color-value (gen/choose 1 8))))

;;; ----------------------------------------------------------------------------
;;; Important modifier generators

(def gen-important-class
  "Class with important modifier (either prefix or suffix)."
  (gen/let [base gen-base-class
            style (gen/elements [:prefix :suffix])]
    (case style
      :prefix (str "!" base)
      :suffix (str base "!"))))

;;; ----------------------------------------------------------------------------
;;; Content utility generators

(def gen-content-value
  (gen/elements ["['hello']" "[attr(data-content)]" "[var(--content)]" "['']"]))

(def gen-conflicting-content
  (gen/fmap (fn [[v1 v2]] [(str "content-" v1) (str "content-" v2)])
            (gen/tuple gen-content-value gen-content-value)))

;;; ----------------------------------------------------------------------------
;;; Cross-group conflict generators (hierarchical specificity)

(def gen-inset-hierarchy
  "Inset overrides inset-x/inset-y, which override individual sides."
  (gen/let [v1 gen-spacing
            v2 gen-spacing]
    (gen/elements
     [[(str "inset-x-" v1) (str "inset-" v2)]     ; inset wins
      [(str "inset-y-" v1) (str "inset-" v2)]     ; inset wins
      [(str "right-" v1) (str "inset-x-" v2)]     ; inset-x wins
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
  (for-all [classes gen-tailwind-class-list]
    (let [once  (resolve-classes classes)
          twice (resolve-classes [once])]
      (= once twice))))

(defspec output-is-subset-of-input num-tests
  (for-all [classes gen-tailwind-class-list]
    (let [result (result-classes (resolve-classes classes))
          input  (set classes)]
      (set/subset? result input))))

(defspec last-color-wins num-tests
  (for-all [[c1 c2] gen-conflicting-colors]
    (str/includes? (resolve-classes [c1 c2]) c2)))

(defspec last-spacing-wins num-tests
  (for-all [[c1 c2] gen-conflicting-spacing]
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
  (for-all [base gen-base-class
            mod  gen-modifier]
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
            v1     gen-spacing
            v2     gen-spacing]
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
