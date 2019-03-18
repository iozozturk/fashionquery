# Assessment Assignment: Fancy Dress Search
For this assignment we ask you to build a web service that implements search functionality over a data set of dresses and ratings.

The solution can be a REST service, but other options are valid as well as long as it is possible to make individual remote calls to the service (e.g. gRPC).

Note that the data set is not provided to you as a whole, but instead the dresses and ratings are incrementally streamed on a Kafka topic. The details about how to set this up and what the messages look like are described later in this document.

## Requirements

The following requirements must be minimally met.

### Query Endpoint
The service must have at least one endpoint that can be used for executing a search query and retrieving the results. The arguments or payload passed to this endpoint will likely depend on the available query options in your implementation (see below). The result set returned from searches must contain all information about the dresses that's available to you.

### Streaming Updates
The service must update its search index in a streaming fashion based on newly incoming messages that create new entries or update existing entries. These new or updated entries must be reflected in the search results for queries that happen at some point in time after receiving a create / update message.

### Query Options
There are many ways to perform search queries. Examples include:
- text based search
- image based search
- faceted search (drill down based on different properties)

For this assignment, we don't specify how the queries should be constructed. You are free to do what makes sense for you. As part of your solution, please state your assumptions about how the users of the service will be querying for dresses.

### Popularity Ranking
It must be possible to rank the search results by popularity. We will loosely define popularity rank as the rank based on the trendiness or demand for the dresses. There are multiple pieces of information that you can use to determine popularity, including but not necessarily limited to:
- the ratings that exist for particular dresses
- how often a dress has been part of a search result in recent history
- possible popularity of common properties of dresses (e.g. the ratings for all dresses of a brand)
- it is possible to create additional endpoints on your service that allow to provide the service with signals that are important for popularity (e.g. click through after a search)

As part of your solution, please provide the definition of popularity that you chose to implement. Also state your assumptions about the users of the service that led to this definition.

*Note this one strict requirement*: your system must always be able to provide a ranking for dresses. Also when there are dresses in the result set that haven't received any ratings or previous searches.

## Dataset
Of course you need a dataset with dresses to implement this service. As part of the assignment, you receive a Python script that produces data for dresses and dress ratings on two different Kafka topics. You will build a Kafka consumer to consume the data for dresses and ratings required to build the search service.

The producer scripts sends messages on two separate topics: `dresses` and `ratings`. These messages contain information on individual dresses and dress ratings (1 to 5 stars) respectively.

### Message formats
All messages on Kafka contain JSON strings, which are UTF-8 encoded (as per the JSON by specification).

##### Dress message format
Example message:

```json
{
  "status": "CREATED",
  "payload_key": "AX821CA1M-Q11",
  "payload": {
    "id": "AX821CA1M-Q11",
    "images": [
      {
        "large_url": "http://i6.ztat.net/large_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@10.jpg",
        "thumb_url": "http://i6.ztat.net/catalog_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@10.jpg"
      },
      {
        "large_url": "http://i3.ztat.net/large_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@9.jpg",
        "thumb_url": "http://i3.ztat.net/catalog_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@9.jpg"
      }
    ],
    "activation_date": "2016-11-22T15:18:41+01:00",
    "name": "Jersey dress - black",
    "color": "Black",
    "season": "WINTER",
    "price": 24.04,
    "brand": {
      "logo_url": "https://i3.ztat.net/brand/9b3cabce-c405-44d7-a62f-ee00d5245962.jpg",
      "name": "Anna Field Curvy"
    }
  },
  "timestamp": 1487593122542
}
```

The `status` field tells you whether the message contains a creation or a update of a dress using the values `CREATED` or `UPDATED` respectively. The `payload_key` will always have the same value as the `id` field of the contained dress. We expect all other fields to be sufficiently self explanatory.

##### Rating message format
Example message:

```json
{
  "status": "CREATED",
  "payload_key": "c29b98c2-00fb-4766-938e-9e511d5f5c55",
  "payload": {
    "rating_id": "c29b98c2-00fb-4766-938e-9e511d5f5c55",
    "dress_id": "NM521C00M-Q11",
    "stars": 1
  },
  "timestamp": 1487596717302
}
```

Unlike dresses, ratings are never updated, so you can safely ignore that scenario.

## Assignment Prerequisites
The assignment has some prerequisites.

### Prerequisite 1: Setup Kafka

##### Using Docker Compose

You can run Kafka by using the provided `docker-compose.yml` file. Simply run the following command:

    docker-compose up
    
If you want to reset Kafka to a clean slate, `docker-compose rm` will do that.

##### Installing locally
While Kafka is intended to be a distributed system, *you are advised not to build a cluster*. Instead, focus on a local installation on your own machine; this will suffice for the assignment. The starting point for Kafka should probably be the [project website](http://kafka.apache.org/).

Make sure your (local) installation works by using Kafka's built in console producer and consumer on a test topic.

### Prerequisite 2: Run the producer script
In the assessment's archive, you'll find one Python source file: `producer.py`. This is the entry point for the Kafka producer that publishes the messages. The script reads an environment variable named `KAFKA_HOST_PORT` to discover the Kafka broker server to connect to. E.g.:

    KAFKA_HOST_PORT=localhost:9092 python producer.py

The producer script publishes messages onto Kafka topics named `dresses` and `ratings` (this is hard coded).

**NOTE:** The producer script is stateful and keeps a local file with some program state. This file is saved at `/tmp/app_state.p`. The state in this file influences the behaviour of the producer script. If you want to start with a clean slate (usually when you clean out the Kafka topics), then delete this file before starting the producer again.

#### Python requirements
The producer script requires Python3 to run. If you are not familiar with Python, there are several ways to get it up and running. On a Mac, you can use Homebrew to install Python 3 (`brew install python3`). On most Linux distributions, Python 3 will be available in the package manager. On all platforms, you can use the Anaconda Python distribution, available from the [Contimuum website](https://www.continuum.io/downloads). In all cases, you will also need the `pip` Python package manager to install additional dependencies (Anaconda has all of this out of the box). The producer script needs the following packages installed to work:

- kafka-python
- numpy (on some platforms, this needs additional system level dependencies, such as Fortran and Atlas)
- click

(So, `pip install numpy click kafka-python`)

## Assignment Interpretation and Implementation

The essence of the assignment is quite strictly defined: the solution needs to consume dresses on a Kafka topic and provide a service for searching the data set. However, the requirements on popularity ranking and search query options leave room for interpretation. While it is possible to dive into extensive research on effective search systems, we encourage you to provide your own assumption about the users of your service and come up with a working solutions which you can incrementally improve on to best match your assumption. Favour a working solution over a long list of TODOs.

### Implementation
For the implementation, use a mainstream programming language; for now we'll limit these to Java, Scala or Python.

Since the messages are produced on a Kafka topic, your solution will include a Kafka consumer. Several options exist for consuming messages from Kafka and doing subsequent processing; we advise to start simple with basic Kafka consumer code.

Finally, you will likely need a search index for this assignment. You are free to choose an implementation of your liking. Some options include:
- ElasticSearch
- SOLR
- Lucene

There might be others that we don't know about as well.

### Evaluation
The evaluation of your solution is based on a presentation given by you for two members of our team and on the solution itself. Please provide us with the working solution beforehand (short notice is OK, doesn't have to be many days before).

First and foremost, focus on a working and complete solution. This implies that in order to reach all the functionality given possible time constraints, it's possible to you need to take a shortcut here or there. Also, it likely makes sense to start with the simplest possible solution and incrementally make improvements where you think they are most valuable.

While the given problem is seemingly simple, don't treat it as just a programming exercise for the sake of validating that you know how to write code (in that case, we'd have asked you to write Quicksort or some other very well defined problem). Instead, treat it as if there will be a real user depending on this solution for searching the data set. Obviously, you'd like to start with a simple solution that works, but we value evidence that you have thought about the problem and perhaps expose ideas for improvement upon this.

The goal of this assignment and the presentation is to assess candidates' skills in the following areas:

- Coding productivity
- Software development
- Computer science
- Operations / systems management
- Distributed systems

While not all of the above might be fully covered by this assignment, we believe we can do a decent job of assessing these based on the solution, the presentation and subsequent Q&A after the presentation.

Apart from the problem interpretation, we value evidence of solid software engineering principles, such as testability, separation of concerns, fit-for-production code, etc.

Finally, we ask you not to share your solution using a publicly visible link (e.g. public Github repo). This makes it easier for us to reuse assignments. BTW: note that Bitbucket provides unlimited private repos on free accounts.

## Note
If you have any questions or want clarification on the requirements, please email dmitry.ivanov@fashiontrade.com.
