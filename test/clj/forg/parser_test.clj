(ns forg.parser-test
  (:require
   [clojure.test :as t]
   [forg.parser :as p]))

(def parse (p/parser-with-normalization p/parser {:autobox? false}))

(t/deftest t6-1
  (t/is (= (parse "****** TODO [#A] A new header :category:")
           {:t6 {:status "TODO",
                 :priority "A",
                 :action "A new header ",
                 :category "category"}})))
