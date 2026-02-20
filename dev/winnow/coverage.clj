(ns winnow.coverage
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pp]
   [clojure.set :as set]
   [clojure.string :as str]
   [winnow.classify :as classify]
   [winnow.config :as config]
   [winnow.trie :as trie]))

;;; ----------------------------------------------------------------------------
;;; Data loading

(def ^:private baseline-file
  "resources/winnow/tailwind-utilities.edn")

(defn load-upstream
  []
  (let [f (io/file baseline-file)]
    (when (.exists f)
      (:utilities (edn/read-string (slurp f))))))

;;; ----------------------------------------------------------------------------
;;; Classification

(defn- compile-config
  [cfg]
  (assoc cfg :prefix-trie (trie/build (:prefixes cfg))))

(defn- make-config
  []
  (compile-config config/default))

(defn- generate-test-classes
  [utility]
  (let [exact-keys (set (keys (:exact config/default)))]
    (cond
      ;; If it's in the exact map, test it directly
      (contains? exact-keys utility)
      [utility]

      ;; Negative utilities need value
      (str/starts-with? utility "-")
      [(str utility "-4") (str utility "-[10px]")]

      ;; Functional utilities - try common value patterns
      :else
      [(str utility "-4")
       (str utility "-auto")
       (str utility "-[10px]")
       (str utility "-red-500")
       (str utility "-[#ff0000]")])))

(defn- utility-covered?
  [cfg utility]
  (let [test-classes (generate-test-classes utility)]
    (some #(classify/classify cfg %) test-classes)))

;;; ----------------------------------------------------------------------------
;;; Coverage analysis

(defn analyze
  []
  (let [upstream (load-upstream)
        cfg      (make-config)
        results  (group-by #(if (utility-covered? cfg %) :covered :missing)
                           upstream)]
    {:total    (count upstream)
     :covered  (count (:covered results))
     :missing  (set (:missing results))
     :coverage (double (/ (count (:covered results)) (count upstream)))}))

(defn- categorize-utilities
  [utilities]
  (let [cfg (make-config)]
    (reduce
     (fn [acc utility]
       (let [test-classes (generate-test-classes utility)]
         (if-let [group (some #(classify/classify cfg %) test-classes)]
           (update acc :by-group update group (fnil conj #{}) utility)
           (update acc :uncategorized conj utility))))
     {:by-group {}
      :uncategorized #{}}
     utilities)))

;;; ----------------------------------------------------------------------------
;;; Generator data

(defn generator-data
  []
  (let [upstream (load-upstream)
        {:keys [by-group uncategorized]} (categorize-utilities upstream)
        static   (into #{}
                       (filter #(or (get-in config/default [:exact %])
                                    (str/ends-with? % "-auto")
                                    (str/ends-with? % "-none")
                                    (str/ends-with? % "-full")))
                       upstream)
        functional (set/difference upstream static)]
    {:static        static
     :functional    functional
     :by-group      by-group
     :uncategorized uncategorized}))

;;; ----------------------------------------------------------------------------
;;; Entry point

(defn -main
  [& args]
  (case (first args)
    "analyze"
    (let [{:keys [total covered missing coverage]} (analyze)]
      (println (format "Coverage: %d/%d (%.1f%%)" covered total (* 100 coverage)))
      (when (seq missing)
        (println "\nMissing utilities:")
        (doseq [u (sort missing)]
          (println " " u))))

    "generators"
    (let [data (generator-data)]
      (println "Static utilities:" (count (:static data)))
      (println "Functional utilities:" (count (:functional data)))
      (println "\nBy group:")
      (doseq [[group utilities] (sort-by (comp str key) (:by-group data))]
        (println (format "  %-30s %d" group (count utilities))))
      (when (seq (:uncategorized data))
        (println "\nUncategorized:" (count (:uncategorized data)))
        (doseq [u (sort (:uncategorized data))]
          (println " " u))))

    "edn"
    (pp/pprint (generator-data))

    (println "Usage: analyze | generators | edn")))

(comment
  (analyze)
  (generator-data)
  (-main "analyze"))
