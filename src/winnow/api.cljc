(ns winnow.api
  "Tailwind CSS class merging for Clojure.

  This library resolves conflicting Tailwind CSS classes by keeping the last
  value for each conflict group. For example, `px-2 px-4` resolves to `px-4`.

  Basic usage:
    (require '[winnow.api :as winnow])
    (winnow/resolve [\"px-2 py-4\" \"px-6\"]) ;; => \"py-4 px-6\"

  Custom colors:
    (def resolve (winnow/make-resolver {:colors #{\"primary\" \"surface\"}}))
    (resolve [\"bg-red-500 bg-primary\"]) ;; => \"bg-primary\"

  The library supports:
    - All Tailwind CSS v3.x and v4.x utilities
    - Arbitrary values: p-[10px], bg-[#ff0000]
    - CSS variables: text-(--my-color), bg-(color:--theme-bg)
    - Modifiers: hover:, focus:, sm:, dark:, etc.
    - Important: !p-4 or p-4!
    - Negative values: -m-4, -inset-x-2

  Resolve accepts a vector of class strings. Each string may contain multiple
  space-separated classes. It returns a single space-separated string with
  conflicts resolved.

  Examples:
    (winnow.api/resolve [\"px-2 py-4\" \"px-6\"])
    ;; => \"py-4 px-6\"

    (winnow.api/resolve [\"text-red-500 hover:text-blue-500\" \"text-green-500\"])
    ;; => \"hover:text-blue-500 text-green-500\"

  Unknown classes (not recognized as Tailwind utilities) pass through unchanged.
  "
  (:refer-clojure :exclude [resolve])
  (:require
   [clojure.string :as str]
   [winnow.classify :as classify]
   [winnow.config :as config]
   [winnow.parse :as parse]
   [winnow.trie :as trie]))

;;; ----------------------------------------------------------------------------
;;; Config compilation

(defn- compile-config
  [config]
  (assoc config :prefix-trie (trie/build (:prefixes config))))

(def ^:private compiled-default
  (delay (compile-config config/default)))

;;; ----------------------------------------------------------------------------
;;; Modifier handling

(defn- order-sensitive?
  [prefixes modifier]
  (some #(str/starts-with? modifier %) prefixes))

(defn- sort-modifiers
  [modifiers order-sensitive]
  (let [sensitive? #(order-sensitive? order-sensitive %)
        sorted     (vec (sort (into [] (remove sensitive?) modifiers)))]
    (loop [i      0
           j      0
           result (transient [])]
      (if (< i (count modifiers))
        (let [m (nth modifiers i)]
          (if (sensitive? m)
            (recur (inc i) j (conj! result m))
            (recur (inc i) (inc j) (conj! result (nth sorted j)))))
        (persistent! result)))))

;;; ----------------------------------------------------------------------------
;;; Class processing

(defn- make-id
  [mod-prefix group]
  (if (seq mod-prefix)
    (str mod-prefix ":" (name group))
    (name group)))

(defn- postfix-conflicts
  [group postfix]
  (when (and postfix (= group :text-size))
    [:leading]))

(defn- strip-prefix
  [class-prefix raw]
  (if class-prefix
    (if (str/starts-with? raw class-prefix)
      [true (subs raw (count class-prefix))]
      [false raw])
    [true raw]))

(defn- process-class
  [config raw]
  (let [[has-prefix? unprefixed] (strip-prefix (:class-prefix config) raw)]
    ;; When prefix is configured, only process classes that have it
    (when has-prefix?
      (let [parsed (parse/parse-class unprefixed)
            group  (classify/classify config (:base parsed))]
        (when group
          (let [mods       (sort-modifiers (:modifiers parsed)
                                           (:order-sensitive-modifiers config))
                mod-prefix (cond-> (str/join ":" mods)
                             (:important? parsed) (str "!"))]
            {:raw              raw
             :group            group
             :id               (make-id mod-prefix group)
             :mod-prefix       mod-prefix
             :extra-conflicts  (postfix-conflicts group (:postfix-at parsed))}))))))

;;; ----------------------------------------------------------------------------
;;; Merge algorithm

(defn- merge-classes
  [config classes]
  (let [processed (into []
                        (comp (mapcat parse/split-classes)
                              (map #(or (process-class config %)
                                        {:raw % :group nil})))
                        classes)]
    (loop [i      (dec (count processed))
           seen   #{}
           result '()]
      (if (neg? i)
        result
        (let [{:keys [raw group id mod-prefix extra-conflicts]} (nth processed i)]
          (if (nil? group)
            (recur (dec i) seen (conj result raw))
            (if (seen id)
              (recur (dec i) seen result)
              (let [conflicts (into (get-in config [:conflicts group] [])
                                    extra-conflicts)
                    new-seen  (into (conj seen id)
                                    (map #(make-id mod-prefix %))
                                    conflicts)]
                (recur (dec i) new-seen (conj result raw))))))))))

;;; ----------------------------------------------------------------------------
;;; Public API

(defn resolve
  ([classes]
   (resolve @compiled-default classes))
  ([config classes]
   {:pre [(vector? classes)]}
   (str/join " " (merge-classes config classes))))

(defn make-resolver
  "Returns a resolve function using a custom config.

  Options:
    :colors - set of custom color names to add (e.g. #{\"surface\" \"primary\"})
    :prefix - class prefix to strip before processing (e.g. \"tw-\" or \"tw:\")"
  [config]
  (let [{:keys [colors prefix]} config
        cfg                     (cond-> config/default
                                  colors (update :colors into colors)
                                  prefix (assoc :class-prefix prefix))
        compiled                (compile-config cfg)]
    (fn [classes]
      (resolve compiled classes))))

(defn supported-patterns
  []
  {:exact    (set (keys (:exact config/default)))
   :prefixes (set (keys (:prefixes config/default)))
   :colors   (:colors config/default)})
