# Instar

[![Build Status](https://travis-ci.org/boxed/instar.svg?branch=master)](https://travis-ci.org/boxed/instar)

> Instar |ˈɪnstɑː|
>
> noun Zoology
>
> A phase between two periods of moulting in the development of an insect larva or other invertebrate animal.
>
> ORIGIN late 19th cent.: from Latin, literally ‘form, likeness’.

Instar is a transformation library for nested data structures. That sounds
pretty abstract, scroll down to the example section for an explanation.

## Installation

Add the following dependency to your `project.clj` file:

[![Clojars Project](http://clojars.org/instar/latest-version.svg)](http://clojars.org/instar)

## Example

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
addresses. We still want to know how many have voted though. This is the
transformation:

```clojure
(transform big-map
  [:votes *] #(assoc %1 :votes (count (:voters %1)))
  [:votes *] #(assoc %1 :did-vote (contains? (:voters %1) "74.125.232.96"))
  [:votes *] #(dissoc % :voters)
  [:votes * :author-ip] dissoc ; This is functionally the same as the dissoc above, but is a bit nicer to read
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

## Other features

Short form for dissoc:

```clojure

(transform {:foo 1, :bar 1}
           [:foo] dissoc)

 =>

 {:bar 1}

```

Short form for setting values:

```clojure

(transform {:foo 1}
           [:foo] "hello")

=>

{:foo "hello"}

```
