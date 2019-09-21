(ns forg.dev.devtools
  (:require
   [devtools.core :as devtools]))

(devtools/set-pref! :disable-advanced-mode-check true)
(devtools/set-pref! :dont-display-banner true)
(devtools/install!)
