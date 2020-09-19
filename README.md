## Solr Streaming Expressions

This demo shows the use cases of Streaming Expressions for data searching, 
aggregating, transforming, analysing data and visualising them. 

### Prerequisites
+ Getting solr locally - Apache Solr (version 8.6)
+ Getting visualisation interpreter locally - Apache Zeppelin (version 0.9)
+ Dataset to work with (Hotel Reviews)

#### Getting Solr locally
To get solr, clone master branch [lucene-solr](https://github.com/apache/lucene-solr) (version 9.0) and switch to (`branch_8_6` which is the desired version we need).

Clone lucene-solr repo: 
```
git clone https://github.com/apache/lucene-solr.git
```

Switch to `branch_8_6`:
```
git fetch origin branch_8_6

git checkout -b branch_8_6 origin/branch_8_6
```

Once you have solr locally, go to `lucene-solr` directory and build the solr. 
```
ant compile
```
If you see an error about Ivy missing while invoking Ant (e.g., .ant/lib does not exist), run `ant ivy-bootstrap` and retry.

Sometimes you may face issues with Ivy (e.g., an incompletely downloaded artifact). Cleaning up the Ivy cache and retrying is a workaround for most of such issues:

`rm -rf ~/.ivy2/cache`

Package solr and prepare it for startup:
```
ant server
```

Run solr in SolrCloud mode from the `solr/` directory:
```
bin/solr start -c
```
Now Solr is running locally on port 8983 (http://localhost:8983/solr)

To stop solr in SolrCloud mode run the following command from the `solr/` directory:
```
bin/solr stop -c
```

#### Getting visualisation interpreter locally - Apache Zeppelin

Download zeppelin from the website. You can download the binary package either with all interpreters or just with Spark interpreter and interpreter net-install script:
```
https://zeppelin.apache.org/download.html
```

Once you download zeppelin, uncompress the file and go to `zeppelin-0.9.0-*` directory. 
Install solr interpreter:
```
./bin/install-interpreter.sh --name solr --artifact com.lucidworks.zeppelin:zeppelin-solr:0.1.6
```

Restart zeppelin:
```
bin/zeppelin-daemon.sh restart
```
Now zeppelin is running locally on port 8080 (http://localhost:8080)

Create interpreter setting in 'Interpreter' menu on Zeppelin GUI
In the interpreter, you provide solr url `solr.baseUrl=http://localhost:8983/solr` 
and collection name `solr.collection=hotels`
![interpreter on GUI](interpreter.png)

Then you can bind the interpreter on your note:
![solrNote on GUI](noteSolr.png)

#### Dataset to work with (Hotel Reviews)
I have taken Hotel Reviews in Europe dataset from Kaggle (https://www.kaggle.com/jiashenliu/515k-hotel-reviews-data-in-europe)
The dataset is in CSV format and located in `resources` package of this repo. 

### Actual Work with Streaming Expressions
+ Index our Hotel Reviews dataset to solr
+ Query, analyze, transform and visualize the dataset

#### Indexing Data
There are 2 ways to index Hotel Reviews dataset: using SolrJ API and with a stream expression.

1. There is `StreamMain` java main class in this repo under `src/main/java` directory. 
Before running the file, you have to specify the absolute file path for Hotel Reviews dataset that you downloaded from Kaggle. 
Then you can run the main method. Basically this class reads the Hotel_Reviews.csv file and index that data to solr. 

2. Data Loading & Visualising CSV file
 We can load the dataset and visualize them using streaming expression. The dataset has to be located under `solr/server/solr/userfiles`.
```
parseCSV(cat("Hotel_Reviews.csv",maxLines=200000))

select(
    parseCSV(cat("Hotel_Reviews.csv",maxLines=200000)), id, Hotel_Name as name_s, Hotel_Address as address_s,
        Average_Score as average_score_f,
        lng as lan_p, lat as lat_p, Tags as tags_s,
        Reviewer_Nationality as reviewer_nationality_s, 
        Negative_Review as negative_review_t, Review_Total_Negative_Word_Counts as negative_review_word_counts_i,
        Positive_Review as positive_review_t, Review_Total_Positive_Word_Counts as positive_review_word_counts_i,
        Reviewer_Score as reviewer_score_f, Review_Date as review_date_dt )

update(hotels, 
       batchSize=10,
       select(parseCSV(cat("Hotel_Reviews.csv",maxLines=200000)), id, Hotel_Name as name_s, Hotel_Address as address_s,
                Average_Score as average_score_f,
                lng as lan_f, lat as lat_f, Tags as tags_s,
                Reviewer_Nationality as reviewer_nationality_s, 
                Negative_Review as negative_review_t, Review_Total_Negative_Word_Counts as negative_review_word_counts_i,
                Positive_Review as positive_review_t, Review_Total_Positive_Word_Counts as positive_review_word_counts_i,
                Reviewer_Score as reviewer_score_f, Review_Date as review_date_tdt))

commit(hotels, batchSize=10,
    update(hotels, 
       batchSize=10,
       select(parseCSV(cat("Hotel_Reviews.csv",maxLines=200000)), id, Hotel_Name as name_s, Hotel_Address as address_s,
                Average_Score as average_score_f,
                lng as lan_f, lat as lat_f, Tags as tags_s,
                Reviewer_Nationality as reviewer_nationality_s, 
                Negative_Review as negative_review_t, Review_Total_Negative_Word_Counts as negative_review_word_counts_i,
                Positive_Review as positive_review_t, Review_Total_Positive_Word_Counts as positive_review_word_counts_i,
                Reviewer_Score as reviewer_score_f, Review_Date as review_date_tdt)))
```

#### Query, analyze, transform and visualize data
List hotels and their locations:
```
rollup(search(hotels, q="*:*", fl="name_s,loc_p", sort="name_s asc", qt="/select", rows=100000), over="name_s, loc_p", count(*))
``` 

List hotels which have more positive reviews:
```
having(rollup(search(hotels, q="*:*", fl="name_s, average_score_f, reviewer_score_f, positive_review_word_counts_i,negative_review_word_counts_i", 
              sort="name_s asc", qt="/select", rows=100000),
        over="name_s, average_score_f, reviewer_score_f", sum(positive_review_word_counts_i), sum(negative_review_word_counts_i)), 
    gt(sum(positive_review_word_counts_i), sum(negative_review_word_counts_i)))
```
And It can be visualized on heatmap the correlation between the average score and the number of negative reviews.


Search for hotels in Paris: 
```
rollup(search(hotels, q="address_s:*Paris*", fl="name_s,loc_p,average_score_f", sort="name_s asc", qt="/select", rows=100000),
  over="name_s, loc_p, average_score_f", count(loc_p))
``` 

List top 10 hotels in Paris with top positive reviews on a map: 
```
let(a=top(n=10,
          rollup(search(hotels, q="address_s:*Paris*", fl="name_s,loc_p,average_score_f,positive_review_word_counts_i", sort="name_s asc", qt="/select",rows=100000),
                 over="name_s, loc_p, average_score_f, positive_review_word_counts_i", count(loc_p)), 
          sort="positive_review_word_counts_i desc"), 
b=latlonVectors(a,field="loc_p"), 
lat=colAt(b,0),
lon=colAt(b,1),
id=col(a,name_s),
zplot(lat=lat,lon=lon,id=id))
```

Top 10 hotels in Paris with the highest average scores:
```
top(n=10, 
    drill(hotels, q="address_s:*Paris*", fl="name_s, address_s, average_score_f", sort="name_s asc", rollup(input(), over="name_s, address_s, average_score_f", 
        count(average_score_f))), 
    sort="count(average_score_f) desc")
```

Analyze data:
```
rollup(cartesianProduct(select(parseCSV(cat("Hotel_Reviews.csv",maxLines=10000)), id, Hotel_Name as name_s, 
Hotel_Address as address_s, analyze(Tags, _t) as tags_s ), tags_s), over="name_s, tags_s", count(tags_s))
```

Top 5 reviewer nationalities and reviewer scores
```
facet2D(hotels, q="*:*", x="reviewer_nationality_s", y="reviewer_score_f", dimensions="5,5", count(*))
```


 

