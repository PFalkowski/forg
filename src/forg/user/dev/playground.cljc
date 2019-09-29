(ns forg.user.dev.playground
  #?(:cljs
     (:require
      [instaparse.core :as insta :include-macros true]))
  #?(:clj
     (:require
      [instaparse.core :as insta]))
  (:require
   [forg.parser :as p]))

(insta/defparser ^:private parser
  ""
  :output-format :enlive)

(def parse (p/parser-with-normalization parser))

(parse "")
