(ns winnow.generators
  (:require
   [clojure.test.check.generators :as gen]
   [winnow.config :as config]
   [winnow.coverage :as coverage]))

;;; ----------------------------------------------------------------------------
;;; Data from upstream

(def upstream-utilities
  (delay (coverage/load-upstream)))

(def exact-utilities
  (delay (set (keys (:exact config/default)))))

(def prefix-utilities
  (delay (set (keys (:prefixes config/default)))))

;;; ----------------------------------------------------------------------------
;;; Value generators

(def gen-spacing-value
  (gen/elements ["0" "1" "2" "3" "4" "5" "6" "8" "10" "12" "16" "20" "24" "32"
                 "px" "0.5" "1.5" "2.5" "3.5" "auto" "full"]))

(def gen-color-name
  (gen/elements (vec (:colors config/default))))

(def gen-shade
  (gen/elements ["50" "100" "200" "300" "400" "500" "600" "700" "800" "900" "950"]))

(def gen-color-with-shade
  (gen/fmap (fn [[c s]] (str c "-" s))
            (gen/tuple gen-color-name gen-shade)))

(def gen-opacity
  (gen/elements ["/0" "/5" "/10" "/25" "/50" "/75" "/90" "/100"]))

(def gen-color-value
  (gen/one-of
   [gen-color-name
    gen-color-with-shade
    (gen/fmap (fn [[c o]] (str c o))
              (gen/tuple gen-color-with-shade gen-opacity))]))

(def gen-arbitrary-length
  (gen/fmap (fn [[n u]] (str "[" n u "]"))
            (gen/tuple (gen/choose 1 100)
                       (gen/elements ["px" "rem" "em" "%" "vh" "vw"]))))

(def gen-hex-color
  (gen/fmap (fn [[r g b]] (format "[#%02x%02x%02x]" r g b))
            (gen/tuple (gen/choose 0 255)
                       (gen/choose 0 255)
                       (gen/choose 0 255))))

;;; ----------------------------------------------------------------------------
;;; Upstream-driven utility generators

(def gen-exact-utility
  (gen/elements (vec @exact-utilities)))

(defn- upstream-prefixes-for-group
  [group]
  (into []
        (comp (filter (fn [[_ v]] (= (:group v) group)))
              (map key))
        (:prefixes config/default)))

(def gen-spacing-prefix
  (gen/elements (vec (concat
                      (upstream-prefixes-for-group :p)
                      (upstream-prefixes-for-group :px)
                      (upstream-prefixes-for-group :py)
                      (upstream-prefixes-for-group :m)
                      (upstream-prefixes-for-group :mx)
                      (upstream-prefixes-for-group :my)
                      (upstream-prefixes-for-group :w)
                      (upstream-prefixes-for-group :h)
                      (upstream-prefixes-for-group :gap)))))

(def gen-color-prefix
  (gen/elements ["text" "bg" "border" "ring" "shadow" "fill" "stroke"
                 "accent" "caret" "placeholder" "divide" "outline"]))

(def gen-spacing-utility
  (gen/fmap (fn [[p v]] (str p "-" v))
            (gen/tuple gen-spacing-prefix
                       (gen/one-of [gen-spacing-value gen-arbitrary-length]))))

(def gen-color-utility
  (gen/fmap (fn [[p c]] (str p "-" c))
            (gen/tuple gen-color-prefix
                       (gen/one-of [gen-color-value gen-hex-color]))))

;;; ----------------------------------------------------------------------------
;;; Modifier generators

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
;;; Compound generators

(def gen-base-class
  (gen/one-of [gen-exact-utility
               gen-spacing-utility
               gen-color-utility]))

(def gen-tailwind-class
  (gen/let [mods (gen/vector gen-modifier 0 2)
            base gen-base-class]
    (if (seq mods)
      (str (clojure.string/join ":" mods) ":" base)
      base)))

(def gen-tailwind-class-list
  (gen/vector gen-tailwind-class 1 8))

;;; ----------------------------------------------------------------------------
;;; Conflict generators (for property testing)

(def gen-conflicting-colors
  (gen/fmap (fn [[p c1 c2]] [(str p "-" c1) (str p "-" c2)])
            (gen/tuple gen-color-prefix gen-color-value gen-color-value)))

(def gen-conflicting-spacing
  (gen/fmap (fn [[p v1 v2]] [(str p "-" v1) (str p "-" v2)])
            (gen/tuple (gen/elements ["p" "m" "w" "h" "gap"])
                       gen-spacing-value
                       gen-spacing-value)))
