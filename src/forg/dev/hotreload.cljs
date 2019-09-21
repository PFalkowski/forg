(ns forg.dev.hotreload
  (:require
   [reagent.core :as reg]
   ["react-native" :as n]
   ["create-react-class" :as react-class]))

(defonce ^:private root-ref (atom nil))
(defonce ^:private root-component-ref (atom nil))

(defn root-with-hotreload!
  [root]
  (let [first-call? (nil? @root-ref)
        root (reg/as-element [root])]
    (reset! root-ref root)
    (if-not first-call?
      (when-let [root @root-component-ref]
        (.forceUpdate ^js root))
      (let [Root (react-class
                  #js {:componentDidMount
                       (fn []
                         (this-as ^js this
                           (reset! root-component-ref this)))
                       :componentWillUnmount
                       (fn []
                         (reset! root-component-ref nil))
                       :render
                       (fn []
                         (let [body @root-ref]
                           (if (fn? body)
                             (body)
                             body)))})]
        (n/AppRegistry.registerComponent "forg" (fn [] Root))))))
