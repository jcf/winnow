(ns winnow.classify
  (:require
   [clojure.string :as str]
   [winnow.trie :as trie]))

;;; ----------------------------------------------------------------------------
;;; Value detection helpers

(def ^:private position-keywords
  #{"bottom" "center" "left" "left-bottom" "left-top"
    "right" "right-bottom" "right-top" "top"})

(def ^:private text-size-keywords
  #{"xs" "sm" "base" "lg" "xl"})

(def ^:private font-weight-keywords
  #{"thin" "extralight" "light" "normal" "medium"
    "semibold" "bold" "extrabold" "black"})

(def ^:private shadow-size-keywords
  #{"xs" "sm" "md" "lg" "xl" "none" "inner"})

(def ^:private ring-width-keywords
  #{"inset"})

(def ^:private border-width-keywords
  #{})

(def ^:private decoration-thickness-keywords
  #{"auto" "from-font"})

;;; ----------------------------------------------------------------------------
;;; Value type predicates

(defn- arbitrary?
  [s]
  (and (>= (count s) 2)
       (str/starts-with? s "[")
       (str/ends-with? s "]")))

(defn- variable?
  [s]
  (and (>= (count s) 2)
       (str/starts-with? s "(")
       (str/ends-with? s ")")))

(defn- content
  [s]
  (when (or (arbitrary? s) (variable? s))
    (subs s 1 (dec (count s)))))

(defn- type-hint
  [s]
  (when-let [c (content s)]
    (when-let [idx (str/index-of c ":")]
      (when (pos? (long idx))
        (let [prefix (subs c 0 idx)]
          ;; Allow lowercase identifiers or CSS custom properties (--name)
          (when (or (re-matches #"^[a-z][a-z-]*$" prefix)
                    (re-matches #"^--[a-zA-Z0-9_-]+$" prefix)
                    (re-matches #"^-[a-z][a-z-]*$" prefix))
            prefix))))))

;;; ----------------------------------------------------------------------------
;;; Color detection
;;;
;;; Colors are recognized only when:
;;; 1. The value is in the configured :colors set
;;; 2. The value follows color-shade pattern (e.g., red-500 where red is known)
;;; 3. Arbitrary hex color: [#fff], [#ff0000]
;;; 4. Type-hinted arbitrary: [color:var(--x)]
;;; 5. Type-hinted variable: (color:--my-var)
;;;
;;; Unknown color names require configuration via make-resolver.

(defn- strip-postfix
  [^String s]
  (if (or (arbitrary? s) (variable? s))
    s
    (if-let [idx (str/index-of s "/")]
      (subs s 0 idx)
      s)))

(defn- contains-digit?
  [^String s]
  #?(:clj  (some #(Character/isDigit ^char %) s)
     :cljs (boolean (re-find #"\d" s))))

(defn- text-size?
  [text-sizes ^String s]
  (let [s (strip-postfix s)]
    (or (arbitrary? s)
        (variable? s)
        (contains-digit? s)
        (text-size-keywords s)
        (text-sizes s))))

(defn- font-weight?
  [^String s]
  (or (arbitrary? s)
      (variable? s)
      (parse-long s)
      (font-weight-keywords s)))

(defn- shadow-size?
  [^String s]
  (let [s (strip-postfix s)]
    (or (arbitrary? s)
        (variable? s)
        (contains-digit? s)
        (shadow-size-keywords s))))

(defn- ring-width?
  [^String s]
  (or (arbitrary? s)
      (variable? s)
      (parse-long s)
      (ring-width-keywords s)))

(defn- border-width?
  [^String s]
  (or (arbitrary? s)
      (variable? s)
      (parse-long s)
      (border-width-keywords s)))

(defn- decoration-thickness?
  [^String s]
  (or (arbitrary? s)
      (variable? s)
      (parse-long s)
      (decoration-thickness-keywords s)))

(defn- color-name?
  [colors ^String s]
  (let [s (strip-postfix s)]
    (or (colors s)
        (when-let [idx (str/last-index-of s "-")]
          (colors (subs s 0 idx))))))

(defn- hex-color?
  [s]
  (when (arbitrary? s)
    (when-let [c (content s)]
      (re-matches #"^#[0-9a-fA-F]{3,8}$" c))))

(defn- color?
  [colors s]
  (or (color-name? colors s)
      (hex-color? s)
      (and (arbitrary? s) (= "color" (type-hint s)))
      (and (variable? s) (= "color" (type-hint s)))))

(defn- color-or-var?
  [colors s]
  (or (color? colors s)
      (and (variable? s) (nil? (type-hint s)))))

;;; ----------------------------------------------------------------------------
;;; Other validators

(defn- image-value?
  [s]
  (when-let [c (content s)]
    (or (str/starts-with? c "url(")
        (str/starts-with? c "linear-gradient(")
        (str/starts-with? c "radial-gradient(")
        (str/starts-with? c "conic-gradient("))))

(defn- type-hint-match?
  [hints ^String value]
  (when-let [hint (or (type-hint value)
                      (when (variable? value) "any"))]
    (contains? hints hint)))

(defn- percent?
  [s]
  (let [s (if (arbitrary? s) (content s) s)]
    (and (str/ends-with? s "%")
         (parse-double (subs s 0 (dec (count s)))))))

;;; ----------------------------------------------------------------------------
;;; Validator dispatch

(defn- resolve-validator
  [config validator-key ^String value]
  (case validator-key
    :color                (color? (:colors config) value)
    :color-or-var         (color-or-var? (:colors config) value)
    :number               (parse-double value)
    :integer              (parse-long value)
    :percent              (percent? value)
    :text-size            (text-size? (or (:text-sizes config) #{}) value)
    :font-weight          (font-weight? value)
    :shadow-size          (shadow-size? value)
    :ring-width           (ring-width? value)
    :border-width         (border-width? value)
    :decoration-thickness (decoration-thickness? value)
    :length               (type-hint-match? #{"length" "size"} value)
    :position             (or (position-keywords value)
                      (type-hint-match? #{"position" "percentage"} value))
    :image                (or (type-hint-match? #{"image" "url"} value)
                      (image-value? value))
    :shadow               (type-hint-match? #{"shadow"} value)
    :family               (type-hint-match? #{"family-name"} value)
    (when-let [pred (get-in config [:validators validator-key])]
      (pred value))))

;;; ----------------------------------------------------------------------------
;;; Arbitrary property detection

(defn- arbitrary-property-group
  [s]
  (when (arbitrary? s)
    (when-let [hint (type-hint s)]
      (keyword (str "arbitrary--" hint)))))

;;; ----------------------------------------------------------------------------
;;; Main classification

(defn classify
  [config ^String base]
  (or
   ;; Arbitrary property like [--my-prop:value]
   (arbitrary-property-group base)

   ;; Exact match
   (get-in config [:exact base])

   ;; Prefix match using trie
   (when-let [[_prefix value prefix-config] (trie/find-prefix (:prefix-trie config) base)]
     (if-let [validators (:validators prefix-config)]
       (some (fn [[validator-key group]]
               (when (resolve-validator config validator-key value)
                 group))
             validators)
       (:group prefix-config)))))
