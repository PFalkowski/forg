(ns forg.root
  (:require
   [forg.dev.hotreload :as h]
   ["react-native" :as n]))

(defn Application
  []
  [:> n/View {}
   [:> n/Text {} "Hello"]])

(defn start!
  {:dev/after-load true}
  []
  (h/root-with-hotreload! Application))

(defn init!
  []
  (start!))
