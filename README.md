# API de Cotação de Moedas - Quarkus

Este projeto monitora a cotação do Dólar (USD-BRL) a cada 35 segundos. Se o valor da cotação atual for maior que o último valor registrado, ele salva a nova informação em um banco de dados PostgreSQL e envia uma notificação para um tópico Apache Kafka.

## Tecnologias

* **Linguagem**: Java 17+
* **Framework**: Quarkus
* **Banco de Dados**: PostgreSQL
* **Mensageria**: Apache Kafka
* **Outros**: Hibernate/Panache, JAX-RS, Kafka Client

## Pré-requisitos

* JDK 17 ou superior
* Maven
* Docker (para rodar o PostgreSQL e o Kafka)

## Como Executar

1.  **Inicie o ambiente com Docker:**

    * **PostgreSQL**:
        ```bash
        docker run --name quotation-db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=123456 -e POSTGRES_DB=quotationdb -p 5432:5432 -d postgres
        ```

    * **Kafka**:
        ```bash
        # (Primeiro inicie o Zookeeper, se ainda não estiver rodando)
        docker run --rm -d --name zookeeper -p 2181:2181 -e ALLOW_ANONYMOUS_LOGIN=yes bitnami/zookeeper:latest

        # Inicie o Kafka
        docker run --rm -d --name kafka -p 9092:9092 --link zookeeper:zookeeper -e KAFKA_CFG_ZOOKEEPER_CONNECT=zookeeper:2181 -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e ALLOW_PLAINTEXT_LISTENER=yes bitnami/kafka:latest
        ```

2.  **Rode a aplicação Quarkus:**
    ```bash
    ./mvnw quarkus:dev
    ```

A aplicação irá iniciar e começar a consultar a cotação automaticamente.

## Fluxo da Aplicação

1.  A cada 35 segundos, o `Scheduler` dispara a tarefa de verificação.
2.  O `QuotationService` utiliza o `CurrencyPriceClient` para buscar a cotação atual do par USD-BRL em uma API externa.
3.  O serviço compara o valor recebido com a última cotação salva no banco de dados PostgreSQL.
4.  **Se o novo valor for maior**, ele é persistido na tabela `quotation`.
5.  Após a persistência, uma mensagem com os dados da nova cotação é enviada para o tópico `quotation` no Kafka.
