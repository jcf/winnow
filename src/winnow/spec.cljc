(ns winnow.spec
  (:require
   [clojure.spec.alpha :as s]
   [winnow.api :as api]
   [winnow.config :as config]))

;;; ----------------------------------------------------------------------------
;;; winnow.config specs

(s/def ::config/colors (s/coll-of string? :kind set?))
(s/def ::config/exact (s/coll-of string? :kind set?))
(s/def ::config/prefixes (s/coll-of string? :kind set?))

;;; ----------------------------------------------------------------------------
;;; winnow.api specs

(s/def ::api/classes (s/coll-of string? :kind vector?))
(s/def ::api/prefix string?)

(s/def ::api/resolver-config
  (s/keys :opt-un [::api/prefix ::config/colors]))

(s/def ::api/supported-patterns
  (s/keys :req-un [::config/exact ::config/prefixes ::config/colors]))

;;; ----------------------------------------------------------------------------
;;; Function specs

(s/fdef api/resolve
  :args (s/alt :unary  (s/cat :classes ::api/classes)
               :binary (s/cat :config map? :classes ::api/classes))
  :ret  string?)

(s/fdef api/make-resolver
  :args (s/cat :config ::api/resolver-config)
  :ret  fn?)

(s/fdef api/supported-patterns
  :args (s/cat)
  :ret  ::api/supported-patterns)

;;; ----------------------------------------------------------------------------
;;; Instrumentation

(defn instrument!
  []
  (require 'clojure.spec.test.alpha)
  ((resolve 'clojure.spec.test.alpha/instrument)
   '[winnow.api/resolve
     winnow.api/make-resolver
     winnow.api/supported-patterns]))
