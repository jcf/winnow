(ns winnow.trie
  (:require
   [winnow.parse :as parse]))

;;; ----------------------------------------------------------------------------
;;; Prefix Trie

(defn build
  "Build a trie from a map of prefix -> config"
  [prefixes]
  (reduce
    (fn [trie [prefix config]]
      (assoc-in trie (conj (vec prefix) :value) config))
    {}
    prefixes))

(defn lookup
  "Find longest matching prefix in trie, return [prefix-end-idx config] or nil.
   Only matches at dash boundaries."
  [trie s]
  (let [len (parse/str-len s)]
    (loop [i 0, node trie, match nil]
      (if (>= i len)
        ;; End of string - only match if we're at a valid boundary
        match
        (let [c (parse/char-at s i)]
          (if (= c \-)
            ;; At dash boundary, record match if node has value, then continue
            (let [new-match (if-let [v (:value node)] [i v] match)]
              (if-let [child (get node c)]
                (recur (inc i) child new-match)
                new-match))
            ;; Regular char, just descend
            (if-let [child (get node c)]
              (recur (inc i) child match)
              ;; Can't continue - return last dash-boundary match
              match)))))))

(defn find-prefix
  "Find longest matching prefix, return [prefix value] or nil"
  [trie s]
  (when-let [[idx config] (lookup trie s)]
    (let [idx (long idx)]
      [(subs s 0 idx) (subs s (inc idx)) config])))
