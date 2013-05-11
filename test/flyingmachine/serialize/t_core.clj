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

(fact "serialize does not include relationships by default"
  (let [serialization (c/serialize t1 ent->topic)]
    serialization => (just {:id 100 :title "First topic"})))

(fact "serialize lets you include relationships"
  (let [t-posts (:posts (c/serialize t1 ent->topic {:include :posts}))]
    (count t-posts) => 2
    t-posts => (contains (map #(c/serialize % ent->post) posts))))

(fact "serialize lets you exclude attributes"
  (c/serialize t1 ent->topic {:exclude [:id]}) =not=> :id)

(fact "you can exclude attributes of relationships"
  (let [t-posts (:posts (c/serialize t1 ent->topic {:include {:posts {:exclude [:id :topic-id]}}}))]
    (count t-posts) => 2
    t-posts => (contains {:content "T1 First post content"})
    t-posts => (contains {:content "T1 Second post content"})))

(fact "you can include relationships of relationships"
  (let [topic (c/serialize t1 ent->topic {:include {:posts {:exclude [:id :topic-id :content]
                                                            :include :topic}}})
        t-posts (:posts topic)
        p-topic (select-keys topic [:id :title])]
    (count t-posts) => 2
    t-posts => (contains {:topic p-topic})))