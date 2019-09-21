(ns forg.user.dev.playground
  #?(:cljs
     (:require
      [instaparse.core :as insta :include-macros true]))
  #?(:clj
     (:require
      [instaparse.core :as insta]))
  (:require
   [forg.parser.core :as fparser]))

(insta/defparser ^:private parser
  ""
  :output-format :enlive)

(def parse (partial fparser/parse* parser))

(parse "")
