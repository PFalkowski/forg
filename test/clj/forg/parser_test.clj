(ns forg.parser-test
  (:require
   [clojure.test :as t]
   [forg.user.dev.playground :as pg]
   [forg.parser :as p]))

(def ^:private use-playground-parser?
  false)

(def parse (p/parser-with-normalization (if-not use-playground-parser?
                                          p/parser
                                          pg/parser)))
(t/deftest t1-1
  (t/is (= [{:t1
            [{:status "TODO"}
             {:priority "A"}
             {:action "A new header "}
             {:category "category"}]}]
           (parse "* TODO [#A] A new header :category:"))))

(t/deftest t1-2-bold
  (t/is (= [{:t1
            [{:status "TODO"}
             {:priority "A"}
             {:action {:bold "A new header"}}
             {:category "category"}]}]
           (parse "* TODO [#A] *A new header* :category:"))))

(t/deftest t1-nest-t2-1
  (t/is (= [{:t1
            [{:status "TODO"}
             {:priority "A"}
             {:action "A new header "}
             {:category "category"}
             {:t2 [{:status "TODO"} {:action "New task"}]}]}]
           (parse "* TODO [#A] A new header :category:
                   ** TODO New task"))))

(t/deftest t1-nest-t2t2-1
  (t/is (= [{:t1
            [{:status "TODO"}
             {:priority "A"}
             {:action "A new header "}
             {:category "category"}
             {:t2 [{:status "TODO"} {:action "New task"}]}
             {:t2 [{:status "TODO"} {:action "New Task the second"}]}]}]
           (parse "* TODO [#A] A new header :category:
                   ** TODO New task
                   ** TODO New Task the second"))))

(t/deftest t1-nest-t2t2-nest-t3-1
  (t/is (= [{:t1
             [{:status "TODO"}
              {:priority "A"}
              {:action "A new header "}
              {:category "category"}
              {:t2 [{:status "TODO"} {:action "New task"}]}
              {:t2
               [{:status "TODO"}
                {:action "New Task the second"}
                {:t3 [{:status "TODO"} {:action "Inner task of nearest t2"}]}]}]}]
           (parse "* TODO [#A] A new header :category:
                   ** TODO New task
                   ** TODO New Task the second
                   *** TODO Inner task of nearest t2"))))

(t/deftest t1-nest-t2-nest-t3-1
  (t/is (= [{:t1
             [{:status "TODO"}
              {:priority "A"}
              {:action "A new header "}
              {:category "category"}
              {:t2 [{:status "TODO"} {:action "New task"}]}]}]
           (parse "* TODO [#A] A new header :category:
                   ** TODO New task"))))

(t/deftest h1-1
  (t/is (= [{:h1 {:sentence "A new header"}}]
           (parse "* A new header"))))

(t/deftest h1-2
  (t/is (= [{:h1 {:sentence {:bold "A new header"}}}]
           (parse "* *A new header*"))))

(t/deftest h1-with-text-1
  (t/is (= [{:h1
             [{:sentence "A new header"}
              {:text-section
               [{:bold "Bold"}
                {:bold "Text"}
                {:EOL "\n"}
                {:text "Something "}
                {:underlined "new"}
                {:text "but fine"}]}]}]
           (parse "* A new header

                  *Bold* *Text*
                  Something _new_ but fine"))))

(t/deftest h1-next-t1
  (t/is (= [{:h1 {:sentence "A new header"}}
            {:t1 [{:status "TODO"} {:action "Super new task"}]}]
           (parse "* A new header
                   * TODO Super new task"))))


(t/deftest h1-next-t2-next-t2
  (t/is (= [{:h1
             [{:sentence "A new header"}
              {:t2 [{:status "TODO"} {:action "Super new task"}]}
              {:t2 [{:status "TODO"} {:action "Super new task"}]}]}]
           (parse "* A new header
                   ** TODO Super new task
                   ** TODO Super new task"))))
