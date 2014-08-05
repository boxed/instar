# Instar

[![Build Status](https://travis-ci.org/boxed/instar.svg?branch=master)](https://travis-ci.org/boxed/instar)

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

And the coup de grâce, the combination of all of the above:

```clojure
(transform {:foo {:bar {:baz 1, :qux 4, :quux 7}}}
           [:foo * *] inc
           [:foobar] "hello"
           [:foo :bar :baz] dissoc)
=> 
{:foo {:bar {:qux 5, :quux 8}}, :foobar "hello"}
```

You can also use instar for getting deep values, either with pairs of [path value] or just the values:

```clojure
(get-in-paths {:foo {:bar {:baz 1, :qux 4, :quux 7}}}
              [:foo * *])
=>
[[[:foo :bar :quux] 7]
 [[:foo :bar :qux] 4]
 [[:foo :bar :baz] 1]]

(get-values-in-paths {:foo {:bar {:baz 1, :qux 4, :quux 7}}}
                     [:foo * *])
=>
[7 4 1]
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
  [:votes *] #(assoc %1 :votes (count (:voters %1)))
  [:votes *] #(assoc %1 :did-vote (contains? (:voters %1) "74.125.232.96"))
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
