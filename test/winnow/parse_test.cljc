(ns winnow.parse-test
  (:require
   #?(:clj  [clojure.test :refer [are deftest is testing]]
      :cljs [cljs.test :refer-macros [are deftest is testing]])
   [winnow.parse :as parse]))

;;; ----------------------------------------------------------------------------
;;; String primitives

(deftest char-at
  (is (= \h (parse/char-at "hello" 0)))
  (is (= \e (parse/char-at "hello" 1)))
  (is (= \o (parse/char-at "hello" 4))))

(deftest str-len
  (is (= 0 (parse/str-len "")))
  (is (= 5 (parse/str-len "hello")))
  (is (= 6 (parse/str-len "[10px]"))))

;;; ----------------------------------------------------------------------------
;;; Bracket syntax detection

(deftest arbitrary?
  (testing "valid arbitrary values"
    (is (parse/arbitrary? "[10px]"))
    (is (parse/arbitrary? "[#fff]"))
    (is (parse/arbitrary? "[var(--x)]"))
    (is (parse/arbitrary? "[color:red]"))
    (is (parse/arbitrary? "[]")))
  (testing "invalid arbitrary values"
    (is (not (parse/arbitrary? "[")))
    (is (not (parse/arbitrary? "[x")))
    (is (not (parse/arbitrary? "x]")))
    (is (not (parse/arbitrary? "(10px)")))
    (is (not (parse/arbitrary? "plain")))))

(deftest variable?
  (testing "valid variable references"
    (is (parse/variable? "(--x)"))
    (is (parse/variable? "(color:--x)"))
    (is (parse/variable? "()"))
    (is (parse/variable? "(var)")))
  (testing "invalid variable references"
    (is (not (parse/variable? "(")))
    (is (not (parse/variable? "(x")))
    (is (not (parse/variable? "x)")))
    (is (not (parse/variable? "[--x]")))
    (is (not (parse/variable? "plain")))))

(deftest bracketed-content
  (testing "extracts content from arbitrary values"
    (is (= "10px" (parse/bracketed-content "[10px]")))
    (is (= "#fff" (parse/bracketed-content "[#fff]")))
    (is (= "color:red" (parse/bracketed-content "[color:red]"))))
  (testing "extracts content from variable references"
    (is (= "--x" (parse/bracketed-content "(--x)")))
    (is (= "color:--x" (parse/bracketed-content "(color:--x)"))))
  (testing "returns nil for non-bracketed"
    (is (nil? (parse/bracketed-content "plain")))
    (is (nil? (parse/bracketed-content "p-4")))))

(deftest arbitrary-content
  (testing "extracts content from arbitrary values"
    (is (= "10px" (parse/arbitrary-content "[10px]")))
    (is (= "#fff" (parse/arbitrary-content "[#fff]"))))
  (testing "returns nil for variable references"
    (is (nil? (parse/arbitrary-content "(--x)"))))
  (testing "returns nil for non-arbitrary"
    (is (nil? (parse/arbitrary-content "plain")))))

;;; ----------------------------------------------------------------------------
;;; Class parsing

(deftest parse-class
  (are [in out] (= out (parse/parse-class in))
    "p-4"
    {:base       "p-4"
     :important? false
     :modifiers  []
     :postfix-at nil}

    "hover:p-4"
    {:base       "p-4"
     :important? false
     :modifiers  ["hover"]
     :postfix-at nil}

    "hover:focus:md:bg-red-500"
    {:base       "bg-red-500"
     :important? false
     :modifiers  ["hover" "focus" "md"]
     :postfix-at nil}

    "p-4!"
    {:base       "p-4"
     :important? true
     :modifiers  []
     :postfix-at nil}

    "!p-4"
    {:base       "p-4"
     :important? true
     :modifiers  []
     :postfix-at nil}

    "hover:!p-4"
    {:base       "p-4"
     :important? true
     :modifiers  ["hover"]
     :postfix-at nil}

    "hover:p-4!"
    {:base       "p-4"
     :important? true
     :modifiers  ["hover"]
     :postfix-at nil}

    "p-[10px]"
    {:base       "p-[10px]"
     :important? false
     :modifiers  []
     :postfix-at nil}

    "p-[10px:20px]"
    {:base       "p-[10px:20px]"
     :important? false
     :modifiers  []
     :postfix-at nil}

    "[color:red]"
    {:base       "[color:red]"
     :important? false
     :modifiers  []
     :postfix-at nil}

    "hover:[color:red]"
    {:base       "[color:red]"
     :important? false
     :modifiers  ["hover"]
     :postfix-at nil}

    "bg-(--my-color)"
    {:base       "bg-(--my-color)"
     :important? false
     :modifiers  []
     :postfix-at nil}

    "text-lg/7"
    {:base       "text-lg/7"
     :important? false
     :modifiers  []
     :postfix-at 7}

    "hover:text-lg/tight"
    {:base       "text-lg/tight"
     :important? false
     :modifiers  ["hover"]
     :postfix-at 7}))

(deftest split-classes
  (are [in out] (= out (parse/split-classes in))
    ;; Basic splitting
    "p-4 m-2 bg-red" ["p-4" "m-2" "bg-red"]

    ;; Multiple spaces
    "  p-4   m-2  "  ["p-4" "m-2"]

    ;; Newlines
    "block\npx-2"    ["block" "px-2"]

    ;; Leading/trailing newlines
    "\nblock\npx-2\n" ["block" "px-2"]

    ;; Mixed whitespace (spaces, newlines, tabs)
    "  block\n\t\n  px-2   \n  py-4  " ["block" "px-2" "py-4"]

    ;; Carriage returns
    "\r  block\n\r  px-2" ["block" "px-2"]

    ;; Empty string
    "" []

    ;; Only whitespace
    "   \n\t  " []))
