# FashionQuery

FashionQuery is a query backend that consumes dress data from Kafka, index them to Elasticsearch and has an endpoint for querying indexed dress data based on rankings.

## Requirements

- Java8 or higher
- Sbt
- Kafka
- Elasticsearch

## Running Backend Service

To bootstrap backend run below command on project root. Service will bind to 8080 port for incoming queries.
> sbt run

## Testing Backend Service

> sbt test

Integration tests require running Elasticsearch.

## Search Examples

#### Search for a dress with keyword

```
$ curl http://localhost:8080/search?query=dress
```

##### Response:
```
[
    {
        "stars_mean": 3.75,
        "score": 0.2459561973810196,
        "images": [
            {
                "thumb_url": "http://i1.ztat.net/catalog_hd/DP/52/9F/00/WN/11/DP529F00W-N11@16.jpg",
                "large_url": "http://i1.ztat.net/large_hd/DP/52/9F/00/WN/11/DP529F00W-N11@16.jpg"
            }
        ],
        "color": "Olive",
        "activation_date": "2016-05-19T15:49:27+02:00",
        "price": 14.4,
        "stars_count": 2,
        "name": "Dress - khaki",
        "season": "WINTER",
        "id": "DP529F00W-N11",
        "brand": {
            "logo_url": "https://i6.ztat.net/brand/8d1dd14d-421f-426c-8e47-e2b8534db96c.jpg",
            "name": "DP Maternity"
        }
    }
]
```

#### Filter dresses with brand name

```
$ curl http://localhost:8080/search?brand=Tommy+Hilfiger
```

##### Response:

```
[
    {
        "stars_mean": 2.5,
        "score": 13.643647193908691,
        "images": [
            {
                "large_url": "http://i1.ztat.net/large_hd/TO/12/1C/04/FK/11/TO121C04F-K11@14.jpg",
                "thumb_url": "http://i1.ztat.net/catalog_hd/TO/12/1C/04/FK/11/TO121C04F-K11@14.jpg"
            }
        ],
        "color": "Blue",
        "activation_date": "2016-08-09T08:45:13+02:00",
        "price": 140.24,
        "stars_count": 1,
        "name": "REGINA - Summer dress - blue",
        "season": "WINTER",
        "id": "TO121C04F-K11",
        "brand": {
            "logo_url": "https://i1.ztat.net/brand/tommyhilfigerto1.jpg",
            "name": "Tommy Hilfiger"
        }
    }
]
```

#### Search for a dress and filter with brand name

```
$ curl http://localhost:8080/search?query=dress&brand=Tommy+Hilfiger
```

##### Response:

```
[
    {
        "stars_mean": 2.5,
        "score": 13.643647193908691,
        "images": [
            {
                "large_url": "http://i1.ztat.net/large_hd/TO/12/1C/04/FK/11/TO121C04F-K11@14.jpg",
                "thumb_url": "http://i1.ztat.net/catalog_hd/TO/12/1C/04/FK/11/TO121C04F-K11@14.jpg"
            }
        ],
        "color": "Blue",
        "activation_date": "2016-08-09T08:45:13+02:00",
        "price": 140.24,
        "stars_count": 1,
        "name": "REGINA - Summer dress - blue",
        "season": "WINTER",
        "id": "TO121C04F-K11",
        "brand": {
            "logo_url": "https://i1.ztat.net/brand/tommyhilfigerto1.jpg",
            "name": "Tommy Hilfiger"
        }
    }
]
```


