(defproject forg "0.0.1-SNAPSHOT"
  :dependencies [[reagent "0.9.0-SNAPSHOT" :exclusions [cljsjs/react
                                                        cljsjs/react-dom
                                                        cljsjs/react-dom-server]]
                 [binaryage/devtools "0.9.10" :scope "test"]
                 [instaparse "1.4.10"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520" :scope "provided" :exclusions [com.google.javascript/closure-compiler-unshaded
                                                                                      org.clojure/google-closure-library]]
                 [thheller/shadow-cljs "2.8.59"]

                 ;; REPL
                 [cider/cider-nrepl "0.22.3" :scope "test"]
                 [com.billpiel/sayid "0.0.18" :scope "test"]
                 [nrepl "0.6.0" :scope "test"]
                 [refactor-nrepl "2.5.0-SNAPSHOT" :scope "test"]]
  :source-paths   ["src"]
  :test-paths     ["test/clj"]
  :profiles {:cljs {:repl-options {:init-ns          shadow.user
                                   :nrepl-middleware [shadow.cljs.devtools.server.nrepl/middleware]}
                    :source-paths ["src"]}})
