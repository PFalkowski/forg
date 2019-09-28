(ns forg.parser.core
  (:refer-clojure :exclude [Box ->Box])
  #?(:cljs
     (:require
      [cljs.pprint :as pprint]
      [instaparse.core :as insta :include-macros true]))
  #?(:clj
     (:require
      [clojure.pprint :as pprint]
      [instaparse.core :as insta]))
  (:require
   [clojure.string :as s]
   [clojure.walk :as walk]))

(def ^:private ^:dynamic *with-box-annotation* true)

(insta/defparser ^:private parser
  "page = settings* <non-todo-text>* t6*
   settings = #'#+.*:.*' <EOL>*
   non-todo-text = (<EOL>*) !settings #'(?:(?!\\*)).+'* <EOL>*
   EOL = '\n' | '\r\n' | #'$'
   SPC = ' '
   priority = (<'[#'> 'A' <']'>) | <'[#'> 'B' <']'> | <'[#'> 'B' <']'>
   status = 'TODO' | 'DONE'
   date = #'[a-zA-Z0-9-:]+'
   weekday = #'\\w+'
   hour = #'\\d{2}:\\d{2}'
   type = 'm' | 'y' | 'd' | 'w'
   times = #'\\d+'
   repeat = <'+'> times type
   time = (<'['> date <SPC>? weekday? <SPC>? hour? <SPC>? repeat? <']'>) | (<'<'> date <SPC>? weekday? <SPC>? hour? <SPC>? repeat? <'>'>)
   name = <':'> (#'[a-zA-Z_]+' ) <':'>
   value = time / #'.*'
   properties = (name <SPC> value <EOL>?)
   <properties-header> = <':PROPERTIES:'> <EOL> properties* <':END:'>
   action = #'[^: \n]*(?: *[^: \n]*)*'
   category = !SPC <':'>? #'[a-zA-Z]*' <':'>
   scheduled = <'SCHEDULED:'> <SPC> time <EOL>?
   visible = time
   deadline = <'DEADLINE:'> <SPC> time <ESPC>*
   scheduled = <'SCHEDULED:'> <SPC> time <ESPC>*
   text = (EOL | #'[^:*/=\n]*' | bold | italic | verbatim | underlined | code | strike-through)*
   underlined = <'_'> #'[^_]*' <'_'>
   strike-through = <'+'> #'[^+]*' <'+'>
   code = <'~'> #'[^~]*' <'~'>
   italic = <'/'> #'[^/]*' <'/'>
   bold = <'*'> #'[^*]*' <'*'>
   <ESPC> = <SPC> | <EOL>
   verbatim = <'='> #'[^=]*' <'='>
   inner =  (h6 | text)*
   sentence = #'[^:*\n]*'
   h6 = <#'\\*{6}'> <SPC> sentence <EOL>? text* h6*
   dates = (deadline | scheduled | visible)*
   t6 = <#'\\*{6}'> <SPC> status? <SPC> priority? <SPC> action <SPC>? category* <ESPC>* properties-header? (dates / inner)*
"
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
    "Passes the object over the sequence of the functions"))

(declare unwrap) ;; To let the box use the unwrap function

(deftype ^:private Box [x _meta]
  #?@(:clj
      [clojure.lang.IObj
       (meta [_] _meta)
       (withMeta [nx nm] (Box. @nx nm))]
      :cljs
      [IMeta
       (-meta [_] _meta)
       IWithMeta
       (-with-meta [nx nm] (Box. @nx nm))])

  #?@(:clj
      [clojure.lang.IDeref
       (deref [this] x)]
      :cljs
      [IDeref
       (-deref [_] x)])

  #?@(:clj
      [clojure.lang.IPersistentCollection
       (empty [this]
               (clojure.core/empty x))])

  #?@(:clj
      [clojure.lang.IPersistentMap
       (containsKey [this k]
                    (clojure.core/contains? x k))
       (assoc [this k v]
              (Box. (clojure.core/assoc x k v) _meta))
       (without [this k]
                (Box. (clojure.core/dissoc x k) _meta))]
      :cljs
      [IAssociative
       (-contains-key? [this k] (contains? x k))
       (-assoc [this k v] (Box. (assoc x k v) _meta))
       IMap
       (-dissoc [this k] (Box. (dissoc x k) _meta))])

  #?@(:clj
      [java.lang.Object
       (toString [this]
                 (str x))
       (equals [this o]
               (= x (unwrap o)))]
      :cljs
      [IEquiv
       (-equiv [this o]
               (= x (unwrap o)))])
  #?@(:clj
      [clojure.lang.Seqable
       (seq [_] (seq x))]),

  #?@(:clj
      [clojure.lang.ILookup
       (valAt [this k]
              (get x k))
       (valAt [this k defval]
              (get x k defval))]
     :cljs
     [ILookup
      (-lookup [this k]
               (get x k))
      (-lookup [this k defval]
               (get x k defval))])

  #?@(:clj
      [clojure.core.protocols.CollReduce
       (coll-reduce [this f]
                    (clojure.core.protocols/coll-reduce x f))
       (coll-reduce [this f xval]
                    (clojure.core.protocols/coll-reduce x f xval))])

  IChainable
  (chain [this vf]
    (as-> (if (sequential? vf) vf (vector vf)) vf
      (reduce (fn [val f] (Box. (f @val) (meta val)))
              (Box. ((first vf) x) _meta)
              (rest vf))))
  (chain [this f1 f2]
    (chain this [f1 f2]))
  (chain [this f1 f2 f3]
    (chain this [f1 f2 f3])) (chain [this f1 f2 f3 f4]
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

(defn- pp-box
  [x]
  (cond-> (with-out-str (pprint/pprint @x))
    (not *print-readably*) (-> (s/replace "\n" "")
                               (s/replace #"(?:(?: ){2,}|\\t*)" " "))))
#?(:cljs
   (extend-protocol IPrintWithWriter
     Box
     (-pr-writer [x writer _]
       (write-all writer (pp-box x)))))

(defmulti tag-normalizer
  "Normalize {:tag something} structures"
  (fn [node] (or (:tag node) :default)))

(defmethod tag-normalizer :default
  [node]
  node)

(defn inner-tag-normalizer
  [inner]
  (if-not (sequential? inner)
    inner
    (-> (reduce (fn [acc [k v]]
                  (assoc acc k (vec (flatten (map vals v)))))
                {}
                (group-by ffirst inner))
        (dissoc nil))))

(defmethod tag-normalizer :task
  [node]
  (cond-> node
    (get-in node [:content :inner] nil) (update-in [:content :inner] inner-tag-normalizer)))

#?(:clj
   (defmethod print-method Box
     [x ^java.io.Writer w]
     (.write w (pp-box x))))

(defmethod pprint/simple-dispatch Box
  [x]
  (if *with-box-annotation*
    (pprint/pprint-logical-block :prefix "#Box " :suffix ""
                                 (pprint/simple-dispatch @x))
    (pprint/simple-dispatch @x)))

(defn parse*
  [aparser s]
  (let [normalize-v-value #(if (= (count %) 1) (first %) (vec %))
        normalize-meta (fn [from] (reduce (fn [acc [k v]]
                                           (into acc [[(keyword (name k)) v]]))
                                         {}
                                         (dissoc (meta from) :instaparse.gll/start-column :instaparse.gll/end-column)))
        group-with-normalization-n-meta (fn [node]
                                          (reduce (fn [acc [k vv]]
                                                    (into acc [[k (as-> (mapv #(with-meta (dissoc (unwrap %) :tag) (meta %)) vv) m-nval
                                                                    (if (= (count m-nval) 1)
                                                                      (box (:content (first m-nval)) (meta (first m-nval)))
                                                                      (mapv #(box (:content %) (meta %)) m-nval)))]]))
                                                  {}
                                                  (group-by (comp :tag unwrap) node)))]
    (->> (insta/add-line-and-column-info-to-metadata s (aparser s))
         (walk/postwalk (fn [node]
                          (as-> (unwrap node) node
                            (if (sequential? node)
                              (cond
                                ;; Like (quote (1 2 3 4))
                                (and (symbol? (unwrap (first (seq node)))))
                                (normalize-v-value (unwrap (second node)))

                                ;; Regular vectors, lists [] '()
                                (not (:content (unwrap (first node))))
                                (normalize-v-value node)

                                ;; [{:content {} :tag a}] | [{:content {} :tag a}, {:content {} :tag b}]
                                (:content (unwrap (first node)))
                                (group-with-normalization-n-meta node)

                                :else node)
                              (cond
                                ;; {:status {}} | {:tag :task} etc..
                                (and (map? node) (meta node))
                                (box (tag-normalizer node) (normalize-meta node))

                                ;; "  String with trailing whitespaces   "
                                (string? node)
                                (s/replace node #" {2,}" "")

                                :else node)))))
         (#(-> % unwrap :content)))))