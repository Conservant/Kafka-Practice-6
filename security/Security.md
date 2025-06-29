#  Безопасность в Kafka

## Настройка защищённого соединения и управление доступом
Цель задания — настроить защищённое SSL-соединение для кластера Apache Kafka из трёх брокеров с использованием Docker Compose, создать новый топик и протестировать отправку и получение зашифрованных сообщений.
Создайте сертификаты для каждого брокера.
Создайте Truststore и Keystore для каждого брокера.
Настройте дополнительные брокеры в режиме SSL. Ранее в курсе вы уже работали с кластером Kafka, состоящим из трёх брокеров. Используйте имеющийся docker-compose кластера и настройте для него SSL.

### Инфраструктура кластера 
Инфраструктура кластера Kafka создается из docker-compose файла
В файле описаны 3 брокера kafka, zookeper

### Создаем сертификаты для каждого из брокеров: 

В корневой директории создадим файл конфигурации для корневого сертификата (Root CA) ca.cnf
Создадим корневой сертификат используется, который будет использоваться для подписания сертификатов брокеров (Root CA) командой:

    openssl req -new -nodes \
    -x509 \
    -days 365 \
    -newkey rsa:2048 \
    -keyout ca.key \
    -out ca.crt \
    -config ca.cnf

Объединим сертификат CA (ca.crt) и его ключ (ca.key) в один файл ca.pem:

    cat ca.crt ca.key > ca.pem

Далее создадим сертификаты для брокеров
Конфигурационные файлы находятся в субдиректориях kafka-*-creds/kafka-*.cnf

Создадим приватные ключи и запросы на сертификаты

    openssl req -new -newkey rsa:2048 -keyout kafka-0-creds/kafka-0.key -out kafka-0-creds/kafka-0.csr -config kafka-0-creds/kafka-0.cnf -nodes
    
    openssl req -new -newkey rsa:2048 -keyout kafka-1-creds/kafka-1.key -out kafka-1-creds/kafka-1.csr -config kafka-1-creds/kafka-1.cnf -nodes
    
    openssl req -new -newkey rsa:2048 -keyout kafka-2-creds/kafka-2.key -out kafka-2-creds/kafka-2.csr -config kafka-2-creds/kafka-2.cnf -nodes

Создадим сертификаты брокеров, подписанные рутовом сертификатом CA

    openssl x509 -req -days 3650 -in kafka-0-creds/kafka-0.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out kafka-0-creds/kafka-0.crt -extfile kafka-0-creds/kafka-0.cnf -extensions v3_req
    
    openssl x509 -req -days 3650 -in kafka-1-creds/kafka-1.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out kafka-1-creds/kafka-1.crt -extfile kafka-1-creds/kafka-1.cnf -extensions v3_req
    
    openssl x509 -req -days 3650 -in kafka-2-creds/kafka-2.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out kafka-2-creds/kafka-2.crt -extfile kafka-2-creds/kafka-2.cnf -extensions v3_req

Создадим PKCS12-хранилища

    openssl pkcs12 -export -in kafka-0-creds/kafka-0.crt -inkey kafka-0-creds/kafka-0.key -chain -CAfile ca.pem -name kafka-0 -out kafka-0-creds/kafka-0.p12 -password pass:my-password
    
    openssl pkcs12 -export -in kafka-1-creds/kafka-1.crt -inkey kafka-1-creds/kafka-1.key -chain -CAfile ca.pem -name kafka-1 -out kafka-1-creds/kafka-1.p12 -password pass:my-password
    
    openssl pkcs12 -export -in kafka-2-creds/kafka-2.crt -inkey kafka-2-creds/kafka-2.key -chain -CAfile ca.pem -name kafka-2 -out kafka-2-creds/kafka-2.p12 -password pass:my-password

Создадим keystore для Kafka

    keytool -importkeystore -deststorepass kafka-password -destkeystore kafka-0-creds/kafka.kafka-0.keystore.pkcs12 -srckeystore kafka-0-creds/kafka-0.p12 -deststoretype PKCS12 -srcstoretype PKCS12 -noprompt -srcstorepass my-password
    
    keytool -importkeystore -deststorepass kafka-password -destkeystore kafka-1-creds/kafka.kafka-1.keystore.pkcs12 -srckeystore kafka-1-creds/kafka-1.p12 -deststoretype PKCS12 -srcstoretype PKCS12 -noprompt -srcstorepass my-password
    
    keytool -importkeystore -deststorepass kafka-password -destkeystore kafka-2-creds/kafka.kafka-2.keystore.pkcs12 -srckeystore kafka-2-creds/kafka-2.p12 -deststoretype PKCS12 -srcstoretype PKCS12 -noprompt -srcstorepass my-password

Создадим truststore для Kafka

    keytool -import -file ca.crt -alias ca -keystore kafka-0-creds/kafka.kafka-0.truststore.jks -storepass my-password -noprompt

    keytool -import -file ca.crt -alias ca -keystore kafka-1-creds/kafka.kafka-1.truststore.jks -storepass my-password -noprompt

    keytool -import -file ca.crt -alias ca -keystore kafka-2-creds/kafka.kafka-2.truststore.jks -storepass my-password -noprompt 

Сохраним пароли

    echo "my-password" > kafka-0-creds/kafka-0_sslkey_creds
    echo "my-password" > kafka-0-creds/kafka-0_keystore_creds
    echo "my-password" > kafka-0-creds/kafka-0_truststore_creds 
    
    echo "my-password" > kafka-1-creds/kafka-1_sslkey_creds
    echo "my-password" > kafka-1-creds/kafka-1_keystore_creds
    echo "my-password" > kafka-1-creds/kafka-1_truststore_creds 
    
    echo "my-password" > kafka-2-creds/kafka-2_sslkey_creds
    echo "my-password" > kafka-2-creds/kafka-2_keystore_creds
    echo "my-password" > kafka-2-creds/kafka-2_truststore_creds 

Настройка завершена

###  Настройка ACL

Создание топиков

    docker exec kafka-topics.sh --bootstrap-server localhost:9092 \
    --create \
    --topic topic-1 \
    --partitions 3 \
    --replication-factor 3

    docker exec kafka-topics.sh --bootstrap-server localhost:9092 \
    --create \
    --topic topic-2 \
    --partitions 3 \
    --replication-factor 3 

Для topic-2 создаем явный запрет на чтение (DENY ACL)

    docker exec kafka-acls.sh --bootstrap-server localhost:9092 --add --deny-principal User:ANONYMOUS --operation Read --topic topic-2 

Разрешаем запись в topic-2

    docker exec kafka-acls.sh --bootstrap-server localhost:9092 --add --allow-principal User:ANONYMOUS --operation Write --topic topic-2
    kafka-acls.sh --bootstrap-server localhost:9092--add --allow-principal User:ANONYMOUS --operation Describe --topic topic-2

Отобразим все ACL

kafka-acls.sh --bootstrap-server localhost:9092 --list




