(ns flyingmachine.serialize.t-core
  (:use midje.sweet)
  (:require [flyingmachine.serialize.core :as c]))

(def t1 {:topic/title "First topic"
         :db/id 100})

(def posts
  [{:post/content "T1 First post content"
    :post/topic t1
    :db/id 101}
   {:post/content "T1 Second post content"
    :post/topic t1
    :db/id 102}])

(c/defserializer ent->post
  (c/attr :id :db/id)
  (c/attr :content :post/content)
  ;; notice that that the second value of attr can be any function
  (c/attr :topic-id (comp :db/id :post/topic))
  (c/has-one :topic
             :serializer flyingmachine.serialize.t-core/ent->topic
             :retriever :post/topic))

(c/defserializer ent->topic
  (c/attr :id :db/id)
  (c/attr :title :topic/title)
  (c/has-many :posts
              :serializer flyingmachine.serialize.t-core/ent->post
              :retriever (fn [_] posts)))

(fact ""
  (c/serialize t1 ent->topic) => {:id 100 :title "First topic"})

