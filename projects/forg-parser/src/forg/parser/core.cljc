(ns forg.parser.core
  #?(:cljs
     (:require
      [instaparse.core :as insta :include-macros true]))
  #?(:clj
     (:require
      [instaparse.core :as insta]))
  (:require
   [clojure.walk :as walk]))

;; sub-heading = sub-level <SPC> [status <SPC>] text tag* [<EOL> content]
;; level = #'\\*+'
;; sub-level = #'\\*{2,}'
;; indent = #'\\s+'
;; list-item = <indent> list-bullet text [<EOL> list-item]
;; list-bullet = ('-' | '+') <SPC>
(insta/defparser ^:private parser
  "page = settings* <non-todo-text>* t7*
   settings = #'#+.*:.*' <EOL>*
   non-todo-text = (<EOL>*) !settings #'(?:(?!\\*)).+'* <EOL>*
   EOL = '\n' | '\r\n' | #'$'
   SPC = ' '
   priority = (<'[#'> 'A' <']'>) | <'[#'> 'B' <']'> | <'[#'> 'B' <']'>
   h7 = #'\\*{7}'
   status = 'TODO' | 'DONE'
   <date-literal> = #'[a-zA-Z0-9 -:]+'
   date = (<'['> date-literal <']'>) | (<'<'> date-literal <'>'>)
   prop-name = <':'> (#'[a-zA-Z_]+' ) <':'>
   prop-val = date / #'.*'
   properties = (prop-name <SPC> prop-val <EOL>?)
   <properties-header> = <':PROPERTIES:'> <EOL> properties* <':END:'>
   action = #'^(?! )[A-Za-z0-9 ]*(?<! )'
   category = !SPC <':'>? #'[a-zA-Z]*' <':'>
   t7 = <h7> <SPC> status <SPC> priority? <SPC> action <SPC>? category* <EOL>? properties-header?
"
  :auto-whitespace :standard
  :output-format :enlive)

(defprotocol IChainable
  (chain
    [this vf]
    [this f1 f2]
    [this f1 f2 f3]
    [this f1 f2 f3 f4]
    [this f1 f2 f3 f4 f5]
    [this f1 f2 f3 f4 f5 f6]
    [this f1 f2 f3 f4 f5 f6 f7]
    [this f1 f2 f3 f4 f5 f6 f7 f8]
    "Provides a generic chain function which passes the unwrapped value over the series of fns and then autoboxes with it's meta"))

(deftype ^:private Box [x _meta]
  clojure.lang.IObj
  (meta [_] _meta)
  (withMeta [nx nm] (Box. @nx nm))

  clojure.lang.IDeref
  (deref [this] x)

  Object
  (toString [this] (str x))

  IChainable
  (chain [this vf]
    (as-> (if (sequential? vf) vf (vec vf)) vf
      (reduce (fn [val f] (Box. (f @val) (meta val)))
              (Box. ((first vf) x) _meta)
              (rest vf))))
  (chain [this f1 f2]
    (chain this [f1 f2]))
  (chain [this f1 f2 f3]
    (chain this [f1 f2 f3]))
  (chain [this f1 f2 f3 f4]
    (chain this [f1 f2 f3 f4]))
  (chain [this f1 f2 f3 f4 f5]
    (chain this [f1 f2 f3 f4 f5]))
  (chain [this f1 f2 f3 f4 f5 f6]
    (chain this [f1 f2 f3 f4 f5 f6]))
  (chain [this f1 f2 f3 f4 f5 f6 f7]
    (chain this [f1 f2 f3 f4 f5 f6 f7]))
  (chain [this f1 f2 f3 f4 f5 f6 f7 f8]
    (chain this [f1 f2 f3 f4 f5 f6 f7 f8])))

(defn box
  ([x]
   (if (instance? Box x)
     x
     (box x (or (meta x) {}))))
  ([x ameta]
   (if (instance? Box x)
     x
     (->Box x ameta))))

(defn unwrap
  [x]
  (if (instance? Box x)
    @x
    x))

(defmethod print-method Box [v ^java.io.Writer w]
  (.write w (str "<Box(" (str @v) ")>")))

(defn parse
  [s]
  (let [normalize-v-value #(if (= (count %) 1) (first %) (vec %))
        normalize-meta (fn [from]
                         (reduce (fn [acc [k v]] (into acc [[(keyword (name k)) v]])) {} (dissoc (meta from) :instaparse.gll/start-column :instaparse.gll/end-column)))
        group-with-normalization-n-meta (fn [node]
                                          (reduce (fn [acc [k vv]]
                                                    (into acc [[k (as-> (mapv #(dissoc (unwrap %) :tag) vv) m-nval
                                                                    (if (= (count m-nval) 1)
                                                                      (:content (first m-nval))
                                                                      (mapv #(:content (unwrap %)) m-nval)))]]))
                                                  {}
                                                  (group-by :tag node)))]
    (->> (insta/add-line-and-column-info-to-metadata s (parser s))
        (walk/postwalk (fn [node]
                         (as-> (unwrap node) node
                           (if (sequential? node)
                             (cond
                               ;; Like (quote (1 2 3 4))
                               (and (symbol? (unwrap (first (seq node)))))
                               (normalize-v-value (unwrap (second node)))

                               ;; Regular vectors, lists
                               (not (:content (unwrap (first node))))
                               (do (println node)
                                   (normalize-v-value node))

                               ;; [:content {}] | [:content [{}]]
                               (and (:content (unwrap (first node))))
                               (group-with-normalization-n-meta node)

                               :else node)
                             (cond
                               ;; {:status {}}
                               (and (map? node) (meta node))
                               (do (println (meta node))
                                   node)

                               :else node)))))
        :content)))

(parse "
******* TODO [#A] Drink vodka with friends :friends:benefit:
:PROPERTIES:
:LAST_REPEAT: [2019-06-24 Mon 08:56]
:Author: Karol
:ABC: 123
:ABd: 22
:ABe: 12t
:ABf: 121
:ABg: 122
:END: ")

(comment
  ())
