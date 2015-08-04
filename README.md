# Instar

[![Build Status](https://travis-ci.org/boxed/instar.svg?branch=master)](https://travis-ci.org/boxed/instar)
[![Examples tested with midje-readme](http://img.shields.io/badge/readme-tested-brightgreen.svg)](https://github.com/boxed/midje-readme)
[![Dependency Status](https://www.versioneye.com/clojure/instar:instar/badge.svg)](https://www.versioneye.com/clojure/instar:instar)


> Instar |ˈɪnstɑː|
>
> noun Zoology
>
> A phase between two periods of moulting in the development of an insect larva or other invertebrate animal.
>
> ORIGIN late 19th cent.: from Latin, literally ‘form, likeness’.

Instar is a library to unify assoc, dissoc and update-in into a coherent and easy to use whole, while also adding wildcard matching on paths. This creates a simple and powerful function for all transformations. There are also functions to extract data based on the same path structure.

## Examples

Nested paths unifies assoc-in, update-in and (the still not in the standard lib) dissoc-in:

```clojure
(def m {:foo {:bar {:baz 1}}})

; Traditional:
(assoc-in  {} [:foo :bar :baz] 7)      => {:foo {:bar {:baz 7}}}
(update-in m  [:foo :bar :baz] inc)    => {:foo {:bar {:baz 2}}}
(update-in m  [:foo :bar] dissoc :baz) => {:foo {:bar {}}}

; With instar:
(transform {} [:foo :bar :baz] 7)      => {:foo {:bar {:baz 7}}}
(transform m  [:foo :bar :baz] inc)    => {:foo {:bar {:baz 2}}}
(transform m  [:foo :bar :baz] dissoc) => {:foo {:bar {}}}
```

Wildcards makes updating multiple values easy:

```clojure
(transform {:foo {:bar {:baz 1, :qux 4}
                  :bar2 {:baz 2, :qux 5}}}
           [:foo * *] inc)
=>
{:foo {:bar {:baz 2, :qux 5},
       :bar2 {:baz 3, :qux 6}}}
```

Besides the flamboyant match-all asterisk, regular expressions can be used for more focused matches:

```clojure
(transform {:foo {:bar {:baz 1, :qux 4}
                  :zip {:baz 2, :qux 5}}}
           [:foo #"^ba" #"^ba"] inc)
=>
{:foo {:bar {:baz 2, :qux 4},
       :zip {:baz 2, :qux 5}}}
```

Key is neither string nor keyword?

Require a more delicate touch?

Clojure functions are treated as match predicates:

```clojure
(transform {:vector [0 1 2 3 4 5 6]}
            [:vector odd?] inc)
=>
{:vector [0 2 2 4 4 6 6]}

(transform {:map {"a" 1, "ab" 2, "abc" 3}}
           [:map (comp even? count)] inc)
=>
{:map {"a" 1, "ab", 3, "abc", 3}}
```

And the coup de grâce, the combination of all of the above:

```clojure
(transform {:foo {:bar {:baz 1, :qux 4, :quux 7}}}
           [:foo * *] inc
           [:foo keyword? #"qu+x"] inc
           [:foobar] "hello"
           [:foo :bar :baz] dissoc)
=>
{:foo {:bar {:qux 6, :quux 9}}, :foobar "hello"}
```

You can also use instar for getting deep values, either with pairs of [path value] or just the values:

```clojure
(get-in-paths {:foo {:bar {:baz 1, :qux 4, :quux 7}}}
              [:foo * *])
=>
[[[:foo :bar :baz] 1]
 [[:foo :bar :qux] 4]
 [[:foo :bar :quux] 7]]


(get-values-in-paths {:foo {:bar {:baz 1, :qux 4, :quux 7}}}
                     [:foo * *])
=>
[1 4 7]
```

### Capture groups

Capture can be performed using the functions `%>`and `%%`, which will replace
the regular argument to any transforming functions (the data being transformed)
with the value captured at that point in the path. Note that multiple
captures can be used, which will then form additional arguments to the function.

The two capture types differ around whether their enclosed path segment becomes
part of the transformation path. The non-resolving form is useful for
capturing values from siblings outside of the fully resolved path.

Use `%%` for non-resolving capture, and `%>` for the resolving variant, as
demonstrated below:


```clojure
(transform {:users [{:name "Dan", :age 23}
                    {:name "Sam", :gender :female}]}
           [:users (%> *) :keys] keys)
=>
{:users [{:name "Dan", :age 23, :keys '(:name :age)}
          {:name "Sam", :gender :female, :keys '(:name :gender)}]}


(:users
  (transform {:users [{:name "Dan", :age 23}
                      {:name "Sam", :gender :female}]
              :aliases {"Dan" ["Dante" "Daniel"]
                        "Sam" ["Samantha", "Samoth"]}}
    [(%% :aliases) :users (%> *) (%% :name)]
      (fn [aliases user name] (assoc user :aliases (aliases name)))))
=>
[{:name "Dan", :age 23,         :aliases ["Dante" "Daniel"]}
 {:name "Sam", :gender :female, :aliases ["Samantha", "Samoth"]}]
```

Note also that the non-resolving capture can conveniently support multiple segments:

```clojure
(transform {:a {:b {:c 42}}}
           [(%% :a :b :c) :d :e :f] vector)
=>
{:a {:b {:c 42}}, :d {:e {:f [42]}}}
```


## Installation

Add the following dependency to your `project.clj` file:

[![Clojars Project](http://clojars.org/instar/latest-version.svg)](http://clojars.org/instar)

## Longer example

This example is based on an actual use case for [atpshowbot](https://github.com/boxed/atpshowbot)

Say we have the following data structure:

```clojure
(def big-map
  {:votes {"title1" {:voters #{"74.125.232.96" "74.125.232.95"}
                     :author "nick1"
                     :author-ip "74.125.232.96"}
           "title2" {:voters #{"74.125.232.96" "74.125.232.95"}
                     :author "nick2"
                     :author-ip "74.125.232.96"}}
   :links [["link" "nick1" "74.125.232.96"]
           ["another link" "nick2" "74.125.232.96"]]})

```

We'd like to send this to a browser, but it's pretty bad to send the users IP
addresses. We still want to know how many have voted though and if the user has already voted.
This is the transformation to do that:

```clojure
(transform big-map
  [:votes (%> *) :votes] #(count (:voters %))
  [:votes (%> *) :did-vote] #(contains? (:voters %) "74.125.232.96")
  [:votes * :voters] dissoc
  [:votes * :author-ip] dissoc
  [:links] #(for [[x y z] %] [x y]))

=>

{:votes {"title1" {:did-vote true,
                   :votes 2,
                   :author "nick1"},
         "title2" {:did-vote true,
                   :votes 2,
                   :author "nick2"}},
 :links [["link" "nick1"]
         ["another link" "nick2"]]}
```
