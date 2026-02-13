(ns winnow.hygiene)

(defn -main
  [& _]
  (let [sw (java.io.StringWriter.)
        pw (java.io.PrintWriter. sw)]
    (binding [*warn-on-reflection* true
              *unchecked-math*     :warn-on-boxed
              *err*                pw]
      (require 'winnow.api :reload-all))
    (let [output (str sw)]
      (if (seq output)
        (do
          (println output)
          (System/exit 1))
        (println "No warnings.")))))
