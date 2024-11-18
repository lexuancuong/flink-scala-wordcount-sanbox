# Flink Word Count with Dynamic Word Filtering

This Apache Flink application demonstrates a word counting system with dynamic word filtering using two Kafka streams:
1. A main input stream for text to be processed
2. A control stream for specifying which words should be counted

## Prerequisites
- Docker and Docker Compose
- SBT (Scala Build Tool)

## Building and Running

1. Build the project:
```bash
sbt clean assembly
```

2. Start the containers:
```bash
docker-compose up -d
```

3. Wait for about 30 seconds for Kafka to be fully ready

4. Submit the Flink job:
```bash
docker-compose exec jobmanager ./bin/flink run -c com.example.WordCount /opt/flink/project/target/scala-2.12/word-count.jar
```

## Testing the Application

1. First, add some accepted words to filter by:
```bash
docker-compose exec kafka kafka-console-producer.sh --broker-list kafka:9092 --topic accepted_words
```
Enter words one per line, for example:
```
hello
world
flink
```

2. Then, produce some input text to be processed:
```bash
docker-compose exec kafka kafka-console-producer.sh --broker-list kafka:9092 --topic input-topic
```
Enter any text to be processed, for example:
```
Hello World! Hello guys! This is a Flink test message
```

3. Eventually, the word counting results will be produced to the output topic:
```bash
docker-compose exec kafka kafka-console-producer.sh --broker-list kafka:9092 --topic output-topic
```
Enter any text to be processed, for example:
```
hello, 2
world, 1
flink, 1
```

## How it Works

- Initially, when no accepted words are defined, the system counts all words
- Once accepted words are added through the `accepted_words` topic, only those words will be counted
- The system maintains counts per word using Flink's state management
- Output format is: (word, status, count) where:
  - word: the actual word being counted
  - status: either "accepted" or "unfiltered"
  - count: the current count for that word

## Stopping the Application

To stop all containers:
```bash
docker-compose down
```
