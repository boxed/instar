(ns instar.core-test
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing test-var)])
  (:require [cemerick.cljs.test :as t]
            [instar.core :refer [transform expand-path expand-path-once]]))

(def test-state1 {:foo {:1 {:q1 1}, :2 {:q2 2}, :3 {:q3 3}}})
(def test-state2 {:foo {:1 {:q1 {:a 1}}, :2 {:q2 2}, :3 {:q3 3}}})

(deftest inner-functions
  (testing "expand-path works properly"

    (is (= (set (expand-path-once test-state1 #js [:foo * *]))
          #{[:foo :1 *] [:foo :2 *] [:foo :3 *]}))

    (is (= #{[:foo :1 :q1] [:foo :2 :q1] [:foo :3 :q1]}
          (expand-path test-state1 [:foo * :q1])))
    (is (= (expand-path test-state1 [:foo *])
          #{[:foo :1] [:foo :2] [:foo :3]}))
    (is (= (expand-path :_ [:foo :bar])
          #{[:foo :bar]}))
    (is (= (expand-path test-state2 [:foo * *])
          #{[:foo :1 :q1] [:foo :2 :q2] [:foo :3 :q3]}))
    (is (= (expand-path test-state2 [:foo * * *])
          #{[:foo :1 :q1 :a]}))))

(deftest simple-transform
  (testing "simplest transform"
    (is (= (transform {} [:test] 1)
           {:test 1}))))
