# flyingmachine/serialize

`flyingmachine/serialize` allows you to define rules for converting
source entities into maps. My primary use case for it is converting
Datomic entities, though you will probably find it useful pretty much
everywhere.

## Example

In this example, assume that you have the following Datomic entities.

If you're not familiar with Datomic, you should still be able to
follow along. When you see `:post/topic #db/id[:db.part/user -100]`,
all that means is "the attribute `:post/topic` refers to the entity
with ID -100". In the case of -100, it's referring to the first topic.

```clojure
[;; first topic
 {:topic/title "First topic"
  :db/id #db/id[:db.part/user -100]}

 {:post/content "T1 First post content"
  :post/topic #db/id[:db.part/user -100]
  :db/id #db/id[:db.part/user -101]}
 {:post/content "T1 Second post content"
  :post/topic #db/id[:db.part/user -100]
  :db/id #db/id[:db.part/user -102]}

 ;; second topic
 {:topic/title "Second topic"
  :db/id #db/id[:db.part/user -200]}

 {:post/content "T2 First post content"
  :post/topic #db/id[:db.part/user -200]
  :db/id #db/id[:db.part/user -201]}
 {:post/content "T2 Second post content"
  :post/topic #db/id[:db.part/user -200]
  :db/id #db/id[:db.part/user -202]}]
```  

We'll be working with the following serializers:

```clojure
(ns example.serializers
  (:use flyingmachine.serialize.core))
(defserializer ent->post
  (attr :id :db/id)
  (attr :content :post/content)
  ;; notice that that the second value of attr can be any function
  (attr :topic-id (comp :db/id :post/topic))
  (has-one :topic
           :serializer example.serializers/ent->topic
           :retriever :post/topic))

(defserializer ent->topic
  (attr :id :db/id)
  (attr :title :topic/title)
  (has-many :posts
            :serializer example.serializers/ent->post
            :retriever #(datomic-magic/all [:post/topic (:db/id %)])))
```

Below are some ways you could serialize the data. Assume that
"datomic-magic" functions magically retrieve datomic entities.

```clojure
=> (use 'flyingmachine.serialize.core)

;; assign the first topic entity to t1
=> (def t1 (datomic-magic/one :topic/title "First topic"))

;; Notice that posts aren't included. This is because relationships
;; aren't included by default. Notice also that the id bears no
;; relation to the temporary Datomic ID above.
=> (serialize t1 example.serializers/ent->topic)
{:id 1000
 :title "First Topic"}


;; When we :include a relationship, it's included in the result. Hey,
;; how bout that wow
=> (serialize t1 example.serializers/ent->topic {:include :posts})
{:id 1000
 :title "First Post"
 :posts [{:id 1001
          :content "T1 First post content"}
         {:id 1002
          :content "T1 Second post content"}]}


;; You can also specify a vector or list of keys to include
=> (serialize t1 example.serializers/ent->topic {:include [:posts]})
{:id 1000
 :title "First Post"
 :posts [{:id 1001
          :content "T1 First post content"}
         {:id 1002
          :content "T1 Second post content"}]}


;; Ugh I'm tired of looking at post ids please exclude them
=> (serialize t1 example.serializers/ent->topic {:include
                                                  {:posts {:exclude [:id]}}})
{:id 1000
 :title "First Post"
 :posts [{:content "T1 First post content"}
         {:content "T1 Second post content"}]}


;; Please don't show me the topic id either, geeze
=> (serialize t1 example.serializers/ent->topic {:exclude [:id]
                                                  :include
                                                  {:posts {:exclude [:id]}}})
{:title "First Post"
 :posts [{:content "T1 First post content"}
         {:content "T1 Second post content"}]}


;; For some completely legitimate reason, I want to include the topic
;; for each post
=> (serialize t1 example.serializers/ent->topic {:exclude [:id]
                                                  :include
                                                  {:posts {:exclude [:id]
                                                           :include :topic}}})
{:title "First Post"
 :posts [{:content "T1 First post content"
          :topic {:id 1000 :title "First Post"}}
         {:content "T1 Second post content"
          :topic {:id 1000 :title "First Post"}}]}
```

I hope this gives you an idea of how to use this library!

## Behind the Scenes

`defserializer` is just a macro that lets you define a data structure
in a manner that's more readable. The end result is a map, like this
one:

```clojure
{:attributes {:title :topic/title, :id :db/id},
 :relationships
 {:posts
  {:retriever #(datomic-magic/all [:post/topic (:db/id %)]),
   :serializer example.serializers/ent->post,
   :arity :many}}}
```

Knowing that, you could use modify the "serializer" however you want.
For example, if you wanted to add 1 to the id, you could do something like

```clojure
(serialize t1 (assoc-in ent->topic [:attributes :id] #(inc (:db/id %))))
```

Isn't that just magical?
