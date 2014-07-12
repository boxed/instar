(ns instar.t-core
  (:use midje.sweet)
  (:use [instar.core]))

(defn noop [x] x)


(def test-state1 {:foo {:1 {:q1 1}, :2 {:q2 2}, :3 {:q3 3}}})
(def test-state2 {:foo {:1 {:q1 {:a 1}}, :2 {:q2 2}, :3 {:q3 3}}})

(fact
  (expand-path test-state1 [:foo :* :q1]) =>
     #{[:foo :1 :q1] [:foo :2 :q1] [:foo :3 :q1]}
  (expand-path test-state1 [:foo :*]) =>
     #{[:foo :1] [:foo :2] [:foo :3]}
  (expand-path :_ [:foo :bar]) =>
     #{[:foo :bar]}
  (expand-path test-state2 [:foo :* :*]) =>
     #{[:foo :1 :q1] [:foo :2 :q2] [:foo :3 :q3]}
  (expand-path test-state2 [:foo :* :* :*]) =>
     #{[:foo :1 :q1 :a]}

  (resolve-paths-for-transform test-state2 [[:foo :* :* :*] noop]) =>
     [[:foo :1 :q1 :a] noop]
)

; This example is based on a use case from https://github.com/boxed/atpshowbot

(def state {
  :votes {"title"
          {:voters #{
                     "74.125.232.96"
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
  :links [
          ["link" "nick1"]
          ["another link" "nick2"]]})
(fact
  (transform state
    [:votes :*] #(assoc %1 :votes (count (:voters %1)))
    [:votes :*] #(assoc %1 :did-vote (contains? (:voters %1) "not-a-matching-ip"))
    [:votes :*] #(dissoc % :voters)
    [:votes :*] #(dissoc % :author-ip)
    [:links] #(for [[x y z] %] [x y])) => target-state
 )
