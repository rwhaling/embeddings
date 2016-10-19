embeddings
----------
This is a simple example project for what we've dubbed the GRASS stack:
- Graphql
- React
- Akka
- Spark
- Sangria
It uses Spark's Implementation of Word2Vec to build a model of text from an input corpus,
then exposes a GraphQL interface with a React frontend.
You can read more about it in [this blog post](http://staging.spantree.net/blog/2016/10/18/spark-akka-sangria.html)

Getting Data
============
You can grab the Brown Corpus here:

Building a jar
==============
Sbt-assembly is configured to build an uber jar with all dependencies,
suitable for stand-alone operation or spark-submit
```
sbt assembly
```

Running it all
==============
You can run the spark job and the server in one pass like this:
```
sbt "run-main net.spantree.embeddings.Main /PATH/TO/BROWN/CORPUS/c*"
```

Running the spark job only
==========================
```
sbt "run-main net.spantree.embeddings.SparkJob /PATH/TO/BROWN/CORPUS/c* model.json"
```

Running the server only
=======================
```
sbt "run-main net.spantree.embeddings.Server model.json"
```
