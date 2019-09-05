(ns forg.util)

(defn reset-changed!
  [ref x]
  (when (not= @ref x)
    (reset! ref x)))
