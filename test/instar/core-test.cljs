(ns instar.core-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [instar.core :refer [transform expand-path get-values-in-paths]]))

(def test-state1 {:foo {:1 {:q1 1}, :2 {:q2 2}, :3 {:q3 3}}})
(def test-state2 {:foo {:1 {:q1 {:a 1}}, :2 {:q2 2}, :3 {:q3 3}}})
(def test-state3 {:foo {:1 [[{:q1 1}]] :2 {:a {:b {:q1 1 :q2 2}}}}})

(deftest inner-functions
  (testing "expand-path works properly"

    (is (= #{[:foo :1 :q1] [:foo :2 :q1] [:foo :3 :q1]}
           (expand-path test-state1 [:foo * :q1])))
    (is (= (expand-path test-state1 [:foo *])
           #{[:foo :1] [:foo :2] [:foo :3]}))
    (is (= (expand-path :_ [:foo :bar])
           #{[:foo :bar]}))
    (is (= (expand-path test-state2 [:foo * *])
           #{[:foo :1 :q1] [:foo :2 :q2] [:foo :3 :q3]}))
    (is (= (expand-path test-state2 [:foo * * *])
           #{[:foo :1 :q1 :a]}))

    ;; sequential
    (is (= (expand-path test-state3 [:foo * * * :q1])
           #{[:foo :2 :a :b :q1] [:foo :1 0 0 :q1]}))

    ;; predicate
    (is (= (expand-path [1 2 3 4 5] [(partial > 4)])
           #{[0] [1] [2] [3]}))
    (is (= (expand-path {-1 :negative 1 :positive} [pos?])
           #{[1]}))

    ;; regex
    (is (= (expand-path {:good {:cat 1 :dog 2 :cat-dog 3} :bad {:catalopse :rawr}}
                        [:good #"cat"])
           #{[:good :cat] [:good :cat-dog]}))

    ;; interaction between matching types
    (is (= (expand-path {:a {"cat" [{"hat" {:b 1} "dot" {:c 3}}
                                    {"lat" {:e 4} "sat" {:f 2}}
                                    {"pot" {:a 3}}
                                    {"bat" {:e 4 :f 3} "dat" {:h 4}}]
                             "rat" [{"fat" {:o 4}}
                                    {"pit" {:p 4} "wat" {:a 1 :h 4}}]}}
                        [:a #"at$" odd? #"at$" *])
           #{[:a "cat" 1 "lat" :e]
             [:a "cat" 1 "sat" :f]
             [:a "cat" 3 "bat" :e]
             [:a "cat" 3 "bat" :f]
             [:a "cat" 3 "dat" :h]
             [:a "rat" 1 "wat" :a]
             [:a "rat" 1 "wat" :h]}))))

(deftest simple-transform
  (testing "simplest transform"
    (is (= (transform {} [:test] 1)
           {:test 1}))))

(deftest get-values
  (testing "get-values-in-paths"
    (is (= (get-values-in-paths {:a {:b [:c :d]
                                     :e #{:f}}
                                 :b {:b [:a :b]
                                     :h #{:f :g}}}
                                [:a * #"\w"]
                                [* :b 1]
                                [* * :f])
           ;; not especially pleased by the nils, as :f cannot key vector
           ;; consider:
           ;; (transform {:a {:b [:c :d]
           ;;                 :e #{:f}}
           ;;             :b {:b [:a :b]
           ;;                 :h #{:f :g}}}
           ;;            [* * :f] 3)
           [:c :d :d :b nil :f nil :f]))))
