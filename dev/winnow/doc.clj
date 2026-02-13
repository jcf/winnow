(ns winnow.doc
  (:require
   [clojure.string :as str]
   [selmer.parser :as selmer]
   [winnow.config :as config])
  (:import
   (java.time LocalDate)
   (java.time.format DateTimeFormatter)))

(defn- format-group-name
  [kw]
  (-> (name kw)
      (str/replace "-" " ")
      str/capitalize))

(defn- group-exact-classes
  [exact-map]
  (->> exact-map
       (group-by val)
       sort
       (mapv (fn [[group classes]]
               {:classes (sort (map key classes))
                :name    (format-group-name group)}))))

(defn -main
  [& _]
  (let [{:keys [exact prefixes colors]} config/default]
    (print
     (selmer/render-file
      "supported-classes.org.selmer"
      {:colors       (sort colors)
       :date         (.format (LocalDate/now) DateTimeFormatter/ISO_LOCAL_DATE)
       :exact-count  (count exact)
       :exact-groups (group-exact-classes exact)
       :prefix-count (count prefixes)
       :prefixes     (sort (keys prefixes))}))))
