(ns winnow.parse-test
  (:require
   #?(:clj  [clojure.test :refer [are deftest]]
      :cljs [cljs.test :refer-macros [are deftest]])
   [winnow.parse :as parse]))

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
