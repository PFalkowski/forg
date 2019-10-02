(defproject forg "0.0.1-SNAPSHOT"
  ;; TODO: Move to seperate profiles?!
  :dependencies [;; Core libraries
                 [reagent                   "0.9.0-SNAPSHOT" :exclusions [cljsjs/react
                                                                          cljsjs/react-dom
                                                                          cljsjs/react-dom-server]]
                 [net.cgrand/xforms         "0.19.1"]
                 [instaparse                "1.4.10"]

                 ;; Clj/s
                 [org.clojure/clojure       "1.10.1"         :scope "provided"]
                 [org.clojure/clojurescript "1.10.520"       :scope "provided" :exclusions [com.google.javascript/closure-compiler-unshaded
                                                                                            org.clojure/google-closure-library]]

                 ;; Benchmarking
                 [com.taoensso/tufte        "2.1.0"          :scope "test"]

                 ;; Compilation tool
                 [thheller/shadow-cljs      "2.8.59"         :scope "test"]

                 ;; Repl & Devtools
                 [nrepl                     "0.6.0"          :scope "test"]
                 [binaryage/devtools        "0.9.10"         :scope "test"]
                 [cider/cider-nrepl         "0.22.3"         :scope "test"]
                 [com.billpiel/sayid        "0.0.18"         :scope "test"]
                 [refactor-nrepl            "2.5.0-SNAPSHOT" :scope "test"]]

  :source-paths   ["src"]

  :test-paths     ["test/clj"]

  :profiles {:cljs {:repl-options {:init-ns          shadow.user
                                   :nrepl-middleware [shadow.cljs.devtools.server.nrepl/middleware]}
                    :source-paths ["src"]}})
