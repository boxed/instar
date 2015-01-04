(ns instar.t-core
  (:refer-clojure :exclude [record?])
  (:use midje.sweet)
  (:require [instar.core :refer [transform
                                 get-in-paths
                                 get-values-in-paths
                                 resolve-paths-for-transform]]))

(def test-state1 {:foo {:1 {:q1 1}, :2 {:q2 2}, :3 {:q3 3}}})
(def test-state2 {:foo {:1 {:q1 {:a 1}}, :2 {:q2 2}, :3 {:q3 3}}})

;; test shims

(defn expand-path [state path]
  (map :path (instar.core/expand-path state path)))

(fact
  (expand-path test-state1 [:foo * :q1]) =>
     [[:foo :1 :q1] [:foo :2 :q1] [:foo :3 :q1]]
  (expand-path test-state1 [:foo *]) =>
     [[:foo :1] [:foo :2] [:foo :3]]
  (expand-path :_ [:foo :bar]) =>
     [[:foo :bar]]
  (expand-path test-state2 [:foo * *]) =>
     [[:foo :1 :q1] [:foo :2 :q2] [:foo :3 :q3]]
  (expand-path test-state2 [:foo * * *]) =>
     [[:foo :1 :q1 :a]]

  (resolve-paths-for-transform test-state2 [[:foo * * *] identity]) =>
     [{:path [:foo :1 :q1 :a], :f identity}]

  (map :path (resolve-paths-for-transform test-state1 [[* * *] identity])) =>
     [[:foo :1 :q1]
      [:foo :2 :q2]
      [:foo :3 :q3]]

  ;; construct using assoc to ensure internal ordering of keys in [:foo :bar]
  (expand-path (assoc nil :bar 2 :foo 1) [*]) =>
     [[:foo] [:bar]])

(fact
 (transform test-state1
            [:foo] dissoc) => {}
 (transform test-state1
            [*] dissoc) => {}
 (transform {}
            [:foo] 1) => {:foo 1}
 ;; Note: matches on path, not value
 (transform [1 2 3 4]
            [odd?] inc) => [1 3 3 5])

(fact
  (into #{} (get-in-paths test-state1
                          [* * *])) =>  #{[[:foo :3 :q3] 3] [[:foo :1 :q1] 1] [[:foo :2 :q2] 2]}
  (into #{} (get-values-in-paths test-state1
                          [* * *])) =>  #{3 1 2}
 )

; This example is based on a use case from https://github.com/boxed/atpshowbot

(def state {
  :votes {"title"
          {:voters #{"74.125.232.96"
                     "74.125.232.95"}
           :author "nick1"
           :author-ip "74.125.232.96"}}
  :links [
          ["link" "nick1" "74.125.232.96"]
          ["another link" "nick2" "74.125.232.96"]]})


(def target-state {
  :votes {"title"
          {:votes 2
           :did-vote false
           :author "nick1"}}
  :links [["link" "nick1"]
          ["another link" "nick2"]]})
(fact
  (transform state
    [:votes *] #(assoc %1 :votes (count (:voters %1)))
    [:votes *] #(assoc %1 :did-vote (contains? (:voters %1) "not-a-matching-ip"))
    [:votes *] #(dissoc % :voters)
    [:votes *] #(dissoc % :author-ip)
    [:links] #(for [[x y z] %] [x y])) => target-state

  ; Same as above...
  (transform state
    [:votes *] #(assoc %1 :votes (count (:voters %1)))
    [:votes *] #(assoc %1 :did-vote (contains? (:voters %1) "not-a-matching-ip"))
    [:votes * :voters] dissoc
    [:votes * :author-ip] dissoc
    [:links] #(for [[x y z] %] [x y])) => target-state

  ; Same as above...
  (transform state
    [:votes *] #(-> %1
                  (assoc :votes (count (:voters %1)))
                  (assoc :did-vote (contains? (:voters %1) "not-a-matching-ip"))
                  (dissoc :voters)
                  (dissoc :author-ip))
    [:links] #(for [[x y z] %] [x y])) => target-state
 )

(fact
  (transform {} [:test] 1) => {:test 1})
