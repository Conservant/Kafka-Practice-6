##  Балансировка топика


1. Создаем новый топик balanced_topic с 8 партициями и фактором репликации 3
   Для этого выполиним команду:

        docker exec kafka-0 kafka-topics.sh --bootstrap-server localhost:9092 --topic balanced_topic --create --partitions 8 --replication-factor 3

   Ожидаемый вывод:

        Created topic balanced_topic.

2. Узнаем текущее распределение партиций

   Выполним команду:

        docker exec kafka-0 kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic balanced_topic

   Возможный вывод после выполения команды:

        Topic: balanced_topic TopicId: HLmTe0VhTx-8cfwhhnUxow PartitionCount: 8 ReplicationFactor: 3 Configs:
        Topic: balanced_topic Partition: 0 Leader: 2 Replicas: 2,0,1 Isr: 2,0,1
        Topic: balanced_topic Partition: 1 Leader: 0 Replicas: 0,1,2 Isr: 0,1,2
        Topic: balanced_topic Partition: 2 Leader: 1 Replicas: 1,2,0 Isr: 1,2,0
        Topic: balanced_topic Partition: 3 Leader: 2 Replicas: 2,1,0 Isr: 2,1,0
        Topic: balanced_topic Partition: 4 Leader: 1 Replicas: 1,0,2 Isr: 1,0,2
        Topic: balanced_topic Partition: 5 Leader: 0 Replicas: 0,2,1 Isr: 0,2,1
        Topic: balanced_topic Partition: 6 Leader: 0 Replicas: 0,2,1 Isr: 0,2,1
        Topic: balanced_topic Partition: 7 Leader: 2 Replicas: 2,1,0 Isr: 2,1,0

3. Создание схемы перераспределения партиций

   "Зайдем" в работающий контейнер и создадим файл reassignment.json для перераспределения партиций.

          docker exec -it kafka-0 bash 

         echo '{
            "version": 1,
            "partitions": [
            {"topic": "balanced_topic", "partition": 0, "replicas": [0, 1], "log_dirs": ["any", "any"]},
            {"topic": "balanced_topic", "partition": 1, "replicas": [1, 2], "log_dirs": ["any", "any"]},
            {"topic": "balanced_topic", "partition": 2, "replicas": [2, 0], "log_dirs": ["any", "any"]},
            {"topic": "balanced_topic", "partition": 3, "replicas": [0, 1], "log_dirs": ["any", "any"]},
            {"topic": "balanced_topic", "partition": 4, "replicas": [1, 2], "log_dirs": ["any", "any"]},
            {"topic": "balanced_topic", "partition": 5, "replicas": [2, 0], "log_dirs": ["any", "any"]},
            {"topic": "balanced_topic", "partition": 6, "replicas": [0, 1], "log_dirs": ["any", "any"]},
            {"topic": "balanced_topic", "partition": 7, "replicas": [1, 2], "log_dirs": ["any", "any"]}
            ]
            }' > /tmp/reassignment.json

   Создать схему перераспределения:

         kafka-reassign-partitions.sh --bootstrap-server localhost:9092 --broker-list "0,1,2" --topics-to-move-json-file "/tmp/reassignment.json" --generate

   Ожидаемый вывод:

         Current partition replica assignment
         {"version":1,"partitions":[]}

   Выполение схемы:

        kafka-reassign-partitions.sh --bootstrap-server localhost:9092 --reassignment-json-file /tmp/reassignment.json --execute 
        Current partition replica assignment
        
        {"version":1,"partitions":[{"topic":"balanced_topic","partition":0,"replicas":[2,0,1],"log_dirs":["any","any","any"]},{"topic":"balanced_topic","partition":1,"replicas":[0,1,2],"log_dirs":["any","any","any"]},{"topic":"balanced_topic","partition":2,"replicas":[1,2,0],"log_dirs":["any","any","any"]},{"topic":"balanced_topic","partition":3,"replicas":[2,1,0],"log_dirs":["any","any","any"]},{"topic":"balanced_topic","partition":4,"replicas":[1,0,2],"log_dirs":["any","any","any"]},{"topic":"balanced_topic","partition":5,"replicas":[0,2,1],"log_dirs":["any","any","any"]},{"topic":"balanced_topic","partition":6,"replicas":[0,2,1],"log_dirs":["any","any","any"]},{"topic":"balanced_topic","partition":7,"replicas":[2,1,0],"log_dirs":["any","any","any"]}]}
        
        Save this to use as the --reassignment-json-file option during rollback
        Successfully started partition reassignments for balanced_topic-0,balanced_topic-1,balanced_topic-2,balanced_topic-3,balanced_topic-4,balanced_topic-5,balanced_topic-6,balanced_topic-7

   Выйдем из контейнера (cmd+d/ctrl+d)

4. Статус перераспределения

        docker exec kafka-0 kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic balanced_topic

    Ожидаемый вывод: 

        Topic: balanced_topic TopicId: HLmTe0VhTx-8cfwhhnUxow PartitionCount: 8 ReplicationFactor: 2 Configs:
        Topic: balanced_topic Partition: 0 Leader: 0 Replicas: 0,1 Isr: 0,1
        Topic: balanced_topic Partition: 1 Leader: 1 Replicas: 1,2 Isr: 2,1
        Topic: balanced_topic Partition: 2 Leader: 2 Replicas: 2,0 Isr: 0,2
        Topic: balanced_topic Partition: 3 Leader: 0 Replicas: 0,1 Isr: 0,1
        Topic: balanced_topic Partition: 4 Leader: 1 Replicas: 1,2 Isr: 1,2
        Topic: balanced_topic Partition: 5 Leader: 0 Replicas: 2,0 Isr: 0,2
        Topic: balanced_topic Partition: 6 Leader: 0 Replicas: 0,1 Isr: 0,1
        Topic: balanced_topic Partition: 7 Leader: 2 Replicas: 1,2 Isr: 1,2

    Заметим, что изменился фактор репликации для партиций и что партиции распределились по двум брокерам каждая

5. Сбой брокера

   Выключим один из брокеров, потушив один из контейнеров

       docker stop kafka-2

   Проверяем состояние топика:

       docker exec kafka-0 kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic balanced_topic

   Ожидаемый вывод:

       Topic: balanced_topic TopicId: HLmTe0VhTx-8cfwhhnUxow PartitionCount: 8 ReplicationFactor: 2 Configs:
       Topic: balanced_topic Partition: 0 Leader: 0 Replicas: 0,1 Isr: 0,1
       Topic: balanced_topic Partition: 1 Leader: 1 Replicas: 1,2 Isr: 1
       Topic: balanced_topic Partition: 2 Leader: 0 Replicas: 2,0 Isr: 0
       Topic: balanced_topic Partition: 3 Leader: 0 Replicas: 0,1 Isr: 0,1
       Topic: balanced_topic Partition: 4 Leader: 1 Replicas: 1,2 Isr: 1
       Topic: balanced_topic Partition: 5 Leader: 0 Replicas: 2,0 Isr: 0
       Topic: balanced_topic Partition: 6 Leader: 0 Replicas: 0,1 Isr: 0,1
       Topic: balanced_topic Partition: 7 Leader: 1 Replicas: 1,2 Isr: 1

   Заметим, что у некоторых партиций ISR

6. Восстановление брокера

   Вновь поднимем контейнер с брокером

        docker compose up -d kafka-2   

   Состояние торпика

        docker exec kafka-0 kafka-topics.sh --bootstrap-server localhost:9092 --describe --topic balanced_topic
        Topic: balanced_topic TopicId: HLmTe0VhTx-8cfwhhnUxow PartitionCount: 8 ReplicationFactor: 2 Configs:
        Topic: balanced_topic Partition: 0 Leader: 0 Replicas: 0,1 Isr: 0,1
        Topic: balanced_topic Partition: 1 Leader: 1 Replicas: 1,2 Isr: 1,2
        Topic: balanced_topic Partition: 2 Leader: 0 Replicas: 2,0 Isr: 0,2
        Topic: balanced_topic Partition: 3 Leader: 0 Replicas: 0,1 Isr: 0,1
        Topic: balanced_topic Partition: 4 Leader: 1 Replicas: 1,2 Isr: 1,2
        Topic: balanced_topic Partition: 5 Leader: 0 Replicas: 2,0 Isr: 0,2
        Topic: balanced_topic Partition: 6 Leader: 0 Replicas: 0,1 Isr: 0,1
        Topic: balanced_topic Partition: 7 Leader: 1 Replicas: 1,2 Isr: 1,2

   Распределение партиций по брокерам вернулось в состояние, которое было сконфигурено.