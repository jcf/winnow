(ns build
  (:require
   [clojure.tools.build.api :as b]
   [deps-deploy.deps-deploy :as deploy]))

(def lib 'dev.jcf/winnow)
(def class-dir "target/classes")
(def src-dirs ["src"])

(def basis (delay (b/create-basis {:project "deps.edn"})))

(defn- jar-file [version]
  (format "target/%s-%s.jar" (name lib) version))

(def pom-data
  [[:licenses
    [:license
     [:name "AGPL-3.0-or-later"]
     [:url "https://www.gnu.org/licenses/agpl-3.0.html"]]]
   [:developers
    [:developer
     [:id "jcf"]
     [:name "James Conroy-Finn"]
     [:email "jcf@jcf.dev"]
     [:url "https://jcf.dev"]]]])

(defn- scm
  [version]
  {:url                 "https://github.com/jcf/winnow"
   :connection          "scm:git:git://github.com/jcf/winnow.git"
   :developerConnection "scm:git:ssh://git@github.com/jcf/winnow.git"
   :tag                 (str "v" version)})

(defn clean
  [_]
  (b/delete {:path "target"}))

(defn jar
  [{:keys [version]}]
  (assert version ":version is required")
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     @basis
                :src-dirs  src-dirs
                :scm       (scm version)
                :pom-data  pom-data})
  (b/copy-dir {:src-dirs   src-dirs
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  (jar-file version)})
  (println "🏗 Built" (jar-file version)))

(defn install
  [{:keys [version] :as opts}]
  (jar opts)
  (b/install {:basis     @basis
              :lib       lib
              :version   version
              :jar-file  (jar-file version)
              :class-dir class-dir})
  (println "💾 Installed" lib version))

(defn deploy
  [{:keys [version] :as opts}]
  (jar opts)
  (deploy/deploy {:installer :remote
                  :artifact  (b/resolve-path (jar-file version))
                  :pom-file  (b/pom-path {:lib lib :class-dir class-dir})})
  (println "🚀 Deployed" lib version "to Clojars"))
