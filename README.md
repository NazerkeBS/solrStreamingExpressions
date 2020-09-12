## Solr Streaming Expressions

This demo shows the use cases of Streaming Expressions for data searching, 
aggregating, transforming, analysing data and visualising them. 

`To get the solr 8.6.1 version`: <br>
[solr](https://github.com/apache/lucene-solr) 

Clone the repo: `https://github.com/apache/lucene-solr.git` <br>

For visualisation, we use Apache Zeppelin [zeppelin](https://zeppelin.apache.org/) <br>
Download zeppelin version `0.9.0` `https://zeppelin.apache.org/download.html`

Clone the repo from Lucidworks: `https://github.com/lucidworks/zeppelin-solr` this is a Solr interpreter for Apache Zeppelin <br >
In the `pom.xml` file, we change the solr version from `7.5.0` to `8.6.1`

From zeppelin directory to start Zeppelin (zeppelin-0.9.0-preview2-bin-all):
`bin/zeppelin-daemon.sh start` 

From the zeppelin-solr directory:
`./bin/install-interpreter.sh --name solr --artifact com.lucidworks.zeppelin:zeppelin-solr:0.1.6`
<br>

Once we set up the environment, we can advance the process. 

Hotel Reviews in Europe Dataset taken from (https://www.kaggle.com/jiashenliu/515k-hotel-reviews-data-in-europe#)
<br>

