(ns winnow.parse
  (:require
   [clojure.string :as str]))

(defn- char-at [s i]
  #?(:clj  (.charAt ^String s i)
     :cljs (nth s i)))

(defn- str-len ^long [s]
  #?(:clj  (.length ^String s)
     :cljs (count s)))

(defn- find-splits
  [s]
  (let [len (str-len s)]
    (loop [i      0
           depth  0
           splits (transient [])]
      (if (>= i len)
        (persistent! splits)
        (let [c (char-at s i)]
          (case c
            \[ (recur (inc i) (inc depth) splits)
            \] (recur (inc i) (dec depth) splits)
            \( (recur (inc i) (inc depth) splits)
            \) (recur (inc i) (dec depth) splits)
            \: (if (zero? depth)
                 (recur (inc i) depth (conj! splits i))
                 (recur (inc i) depth splits))
            (recur (inc i) depth splits)))))))

(defn- split-at-indices
  [s indices]
  (if (empty? indices)
    [s]
    (loop [start        0
           parts        (transient [])
           [idx & more] indices]
      (if idx
        (let [idx (long idx)]
          (recur (inc idx) (conj! parts (subs s start idx)) more))
        (persistent! (conj! parts (subs s start)))))))

(defn- strip-important
  [s]
  (cond
    (str/ends-with? s "!")   [true (subs s 0 (dec (str-len s)))]
    (str/starts-with? s "!") [true (subs s 1)]
    :else                    [false s]))

(defn- find-postfix-position
  [s]
  (let [len (str-len s)]
    (loop [i     0
           depth 0
           pos   nil]
      (if (>= i len)
        pos
        (let [c (char-at s i)]
          (case c
            \[ (recur (inc i) (inc depth) pos)
            \] (recur (inc i) (dec depth) pos)
            \( (recur (inc i) (inc depth) pos)
            \) (recur (inc i) (dec depth) pos)
            \/ (recur (inc i) depth (if (zero? depth) i pos))
            (recur (inc i) depth pos)))))))

(defn parse-class
  [s]
  (let [splits            (find-splits s)
        parts             (split-at-indices s splits)
        base-raw          (peek parts)
        modifiers         (if (> (count parts) 1)
                            (pop parts)
                            [])
        [important? base] (strip-important base-raw)
        postfix-at        (find-postfix-position base)]
    {:modifiers  modifiers
     :important? important?
     :base       base
     :postfix-at postfix-at}))

(defn split-classes
  [s]
  (let [trimmed (str/trim s)]
    (if (str/blank? trimmed)
      []
      (str/split trimmed #"\s+"))))
