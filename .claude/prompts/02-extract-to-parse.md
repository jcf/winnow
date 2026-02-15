# Phase 2: Extract Shared Functions to parse.cljc

## Goal

Eliminate duplicate code by making shared functions public in `parse.cljc`.
Add tests for the newly public functions.

## Duplicated Code

### `char-at` and `str-len`

Defined in both:
- `parse.cljc:5-11`
- `trie.cljc:3-9`

### `arbitrary?`

Defined in both:
- `config.cljc:14-18`
- `classify.cljc:35-39`

### `content`

Defined in both:
- `config.cljc:20-22` (arbitrary only)
- `classify.cljc:47-50` (arbitrary or variable)

## Implementation

### Step 1: Update parse.cljc

Make the existing private functions public and add `variable?`:

```clojure
(ns winnow.parse
  (:require
   [clojure.string :as str]))

;;; ----------------------------------------------------------------------------
;;; String primitives

(defn char-at
  "Returns character at index i in string s."
  [s i]
  #?(:clj  (.charAt ^String s i)
     :cljs (nth s i)))

(defn str-len
  "Returns length of string s."
  ^long [s]
  #?(:clj  (.length ^String s)
     :cljs (count s)))

;;; ----------------------------------------------------------------------------
;;; Bracket syntax detection

(defn arbitrary?
  "Returns true if s is an arbitrary value: [...]"
  [s]
  (and (>= (count s) 2)
       (str/starts-with? s "[")
       (str/ends-with? s "]")))

(defn variable?
  "Returns true if s is a CSS variable reference: (...)"
  [s]
  (and (>= (count s) 2)
       (str/starts-with? s "(")
       (str/ends-with? s ")")))

(defn bracketed-content
  "Returns content inside [] or (), or nil if neither."
  [s]
  (when (and (>= (count s) 2)
             (or (arbitrary? s) (variable? s)))
    (subs s 1 (dec (count s)))))

(defn arbitrary-content
  "Returns content inside [], or nil if not arbitrary."
  [s]
  (when (arbitrary? s)
    (subs s 1 (dec (count s)))))

;;; ----------------------------------------------------------------------------
;;; Class parsing (existing code follows)
```

### Step 2: Update trie.cljc

```clojure
(ns winnow.trie
  (:require
   [winnow.parse :as parse]))

;; Remove local char-at and str-len definitions

;; Update lookup to use parse/char-at and parse/str-len
(defn lookup
  [trie s]
  (let [len (parse/str-len s)]
    (loop [i 0, node trie, match nil]
      (if (>= i len)
        match
        (let [c (parse/char-at s i)]
          ;; ... rest unchanged
```

### Step 3: Update classify.cljc

```clojure
(ns winnow.classify
  (:require
   [clojure.string :as str]
   [winnow.parse :as parse]
   [winnow.trie :as trie]))

;; Remove local arbitrary?, variable?, content definitions

;; Update functions to use parse/arbitrary?, parse/variable?, parse/bracketed-content
(defn- type-hint
  [s]
  (when-let [c (parse/bracketed-content s)]
    (when-let [idx (str/index-of c ":")]
      ;; ... rest unchanged
```

### Step 4: Update config.cljc

```clojure
(ns winnow.config
  (:require
   [clojure.string :as str]
   [winnow.parse :as parse]))

;; Remove local arbitrary? and content definitions

;; Update stroke-width? to use parse functions
(defn- stroke-width?
  [s]
  (or (parse-long s)
      (and (parse/arbitrary? s)
           (let [c (parse/arbitrary-content s)]
             (or (ends-with-unit? c)
                 (parse-double c))))))
```

### Step 5: Add tests

Create or update `test/winnow/parse_test.cljc`:

```clojure
(ns winnow.parse-test
  (:require
   #?(:clj  [clojure.test :refer [deftest is testing]]
      :cljs [cljs.test :refer-macros [deftest is testing]])
   [winnow.parse :as sut]))

(deftest char-at-test
  (is (= \h (sut/char-at "hello" 0)))
  (is (= \o (sut/char-at "hello" 4))))

(deftest str-len-test
  (is (= 0 (sut/str-len "")))
  (is (= 5 (sut/str-len "hello"))))

(deftest arbitrary?-test
  (testing "valid arbitrary values"
    (is (sut/arbitrary? "[10px]"))
    (is (sut/arbitrary? "[#fff]"))
    (is (sut/arbitrary? "[var(--x)]")))
  (testing "invalid arbitrary values"
    (is (not (sut/arbitrary? "[]")))  ; too short content-wise but valid syntax
    (is (not (sut/arbitrary? "[x")))
    (is (not (sut/arbitrary? "x]")))
    (is (not (sut/arbitrary? "(10px)")))))

(deftest variable?-test
  (testing "valid variable references"
    (is (sut/variable? "(--x)"))
    (is (sut/variable? "(color:--x)")))
  (testing "invalid variable references"
    (is (not (sut/variable? "()")))
    (is (not (sut/variable? "(x")))
    (is (not (sut/variable? "[--x]")))))

(deftest bracketed-content-test
  (is (= "10px" (sut/bracketed-content "[10px]")))
  (is (= "--x" (sut/bracketed-content "(--x)")))
  (is (nil? (sut/bracketed-content "plain"))))

(deftest arbitrary-content-test
  (is (= "10px" (sut/arbitrary-content "[10px]")))
  (is (nil? (sut/arbitrary-content "(--x)")))
  (is (nil? (sut/arbitrary-content "plain"))))

(deftest parse-class-test
  (testing "simple class"
    (is (= {:modifiers []
            :important? false
            :base "p-4"
            :postfix-at nil}
           (sut/parse-class "p-4"))))
  (testing "with modifiers"
    (is (= {:modifiers ["hover" "sm"]
            :important? false
            :base "p-4"
            :postfix-at nil}
           (sut/parse-class "hover:sm:p-4"))))
  (testing "important prefix"
    (is (= {:modifiers []
            :important? true
            :base "p-4"
            :postfix-at nil}
           (sut/parse-class "!p-4"))))
  (testing "important suffix"
    (is (= {:modifiers []
            :important? true
            :base "p-4"
            :postfix-at nil}
           (sut/parse-class "p-4!"))))
  (testing "with postfix"
    (let [result (sut/parse-class "text-xl/8")]
      (is (= "text-xl/8" (:base result)))
      (is (= 7 (:postfix-at result))))))

(deftest split-classes-test
  (is (= [] (sut/split-classes "")))
  (is (= [] (sut/split-classes "   ")))
  (is (= ["p-4"] (sut/split-classes "p-4")))
  (is (= ["p-4" "m-2"] (sut/split-classes "p-4 m-2")))
  (is (= ["p-4" "m-2"] (sut/split-classes "  p-4   m-2  "))))
```

## Verification

```sh
just          # Full test suite
just hygiene  # No new warnings
just bench    # No performance regression
```

## Notes

- `[]` is technically valid arbitrary syntax but has no content
- Keep `arbitrary?` checking `>= 2` for the brackets themselves
- The `bracketed-content` function handles both `[]` and `()`
- The `arbitrary-content` function is specific to `[]` only
