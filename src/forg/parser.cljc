(ns forg.parser
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

(insta/defparser parser
  "page = settings* <non-todo-text>* ((h1 / t1) / (h2 / t2) / (h3 / t3) / (h4 / t4) / (h5 / t5) / (h6 / t6) / (h7 / t7))*
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
   action = (italic / bold / verbatim / #'[^: \n]*(?: *[^: \n]*)*')
   category = !SPC <':'>? #'[a-zA-Z]*' <':'>
   scheduled = <'SCHEDULED:'> <SPC> time <EOL>?
   visible = time
   deadline = <'DEADLINE:'> <SPC> time <ESPC>*
   scheduled = <'SCHEDULED:'> <SPC> time <ESPC>*
   text = #'[^:*/_=\n]*'
   text-section = !ESPC (ESPC / bold / italic / verbatim / underlined / code / strike-through / text)*
   underlined = <'_'> !'_' #'[^_]*' <'_'>
   strike-through = <'+'> !'+' #'[^+]*' <'+'>
   code = <'~'> !'~' #'[^~]*' <'~'>
   italic = <'/'> !'/' #'[^/]*' <'/'>
   bold = <'*'> !'*' #'[^*]*' <'*'>
   verbatim = <'='> #'[^=]*' <'='>
   <ESPC> = <SPC> | EOL
   sentence = !status (italic / bold / verbatim / #'[^:*\n]*')
   dates = (deadline | scheduled | visible)*
   <header-inner> = <SPC> sentence <ESPC>* text-section?
   <h1-inner> = (h2 / t2 / h3 / t3 / h4 / t4 / h5 / t5 / h6 / t6 / h7 / t7)*
   <h2-inner> = (h3 / t3 / h4 / t4 / h5 / t5 / h6 / t6 / h7 / t7)*
   <h3-inner> = (h4 / t4 / h5 / t5 / h6 / t6 / h7 / t7)*
   <h4-inner> = (h5 / t5 / h6 / t6 / h7 / t7)*
   <h5-inner> = (h6 / t6 / h7 / t7)*
   <h6-inner> = (h7 / t7)*
   h1 = <#'\\*{1}'> header-inner h1-inner
   h2 = <#'\\*{2}'> header-inner h2-inner
   h3 = <#'\\*{3}'> header-inner h3-inner
   h4 = <#'\\*{4}'> header-inner h4-inner
   h5 = <#'\\*{5}'> header-inner h5-inner
   h6 = <#'\\*{6}'> header-inner h6-inner
   h7 = <#'\\*{7}'> header-inner
   <task-inner> = <SPC> status <SPC> (priority <SPC>)? action <SPC>? category* <ESPC>* properties-header? <ESPC>* dates? <ESPC>*
   <t1-inner> = task-inner (h2 / t2 / h3 / t3 / h4 / t4 / h5 / t5 / h6 / t6 / h7 / t7 / text-section)*
   <t2-inner> = task-inner (h3 / t3 / h4 / t4 / h5 / t5 / h6 / t6 / h7 / t7 / text-section)*
   <t3-inner> = task-inner (h4 / t4 / h5 / t5 / h6 / t6 / h7 / t7 / text-section)*
   <t4-inner> = task-inner (h5 / t5 / h6 / t6 / h7 / t7 / text-section)*
   <t5-inner> = task-inner (h6 / t6 / h7 / t7 / text-section)*
   <t6-inner> = task-inner (h7 / t7 / text-section)*
   <t7-inner> = task-inner text-section*
   t1 = <#'\\*{1}'> !'*' t1-inner
   t2 = <#'\\*{2}'> !'*' t2-inner
   t3 = <#'\\*{3}'> !'*' t3-inner
   t4 = <#'\\*{4}'> !'*' t4-inner
   t5 = <#'\\*{5}'> !'*' t5-inner
   t6 = <#'\\*{6}'> !'*' t6-inner
   t7 = <#'\\*{7}'> !'*' t7-inner
  "
  :output-format :enlive)

(defn- normalize-v-value
  [xs]
  (if (= (count xs) 1) (first xs) (vec xs)))

(defn- wrap-root-node
  [x]
  (when-not (nil? x)
    (if (sequential? x)
      x
      (vector x))))

(defn parser-with-normalization
  [aparser]
  (fn [s]
    (->> (insta/add-line-and-column-info-to-metadata s (aparser s))
         (walk/postwalk (fn [node]
                          (if (sequential? node)
                            (cond
                              ;; Like (quote (1 2 3 4))
                              (and (symbol? (first (seq node))))
                              (normalize-v-value (second node))

                              (= :content (first node))
                              (if (sequential? (second node))
                                [:content (vec (second node))]
                                node)

                              ;; Regular vectors, lists [] '()
                              (not (:content (first node)))
                              (normalize-v-value node)

                              :else node)
                            (cond
                              ;; "  String with trailing whitespaces   "
                              (string? node)
                              (s/replace node #" {2,}" "")

                              (and (map? node) (:tag node))
                              (into {} [[(:tag node) (:content node)]])

                              :else node))))
         :page
         wrap-root-node)))
