(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def lib 'dev.invetica/winnow)
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def src-dirs ["src"])

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     (b/create-basis {:project "deps.edn"})
                :src-dirs  src-dirs
                :scm       {:url "https://github.com/invetica/winnow"
                            :connection "scm:git:git://github.com/invetica/winnow.git"
                            :developerConnection "scm:git:ssh://git@github.com/invetica/winnow.git"
                            :tag (str "v" version)}
                :pom-data  [[:licenses
                             [:license
                              [:name "AGPL-3.0-or-later"]
                              [:url "https://www.gnu.org/licenses/agpl-3.0.html"]]]]})
  (b/copy-dir {:src-dirs   src-dirs
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file})
  (println "Built" jar-file))

(defn install [_]
  (jar nil)
  (b/install {:basis     (b/create-basis {:project "deps.edn"})
              :lib       lib
              :version   version
              :jar-file  jar-file
              :class-dir class-dir})
  (println "Installed" lib version))
