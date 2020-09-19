import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public class StreamMain {

    private static Logger logger = Logger.getLogger(StreamMain.class);

    final static String solrUrl = "http://localhost:8983/solr";
    final static String file = "/Users/naz/workspace/solrStreamingExpression/src/main/resources/Hotel_Reviews.csv";

    public static void main(String[] args) throws IOException, SolrServerException {

        final SolrClient solr = new HttpSolrClient.Builder(solrUrl).build();
        solr.deleteByQuery("hotels", "*:*");

        try (CSVParser parser = new CSVParser(new FileReader(file), CSVFormat.RFC4180.withFirstRecordAsHeader())) {
            StreamSupport.stream(parser.spliterator(), false)
                    .map(convertCSVToSolrDocument())
                    .forEach(addToSolr(solr, "hotels"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        solr.commit("hotels");

    }

    private static Function<CSVRecord, SolrInputDocument> convertCSVToSolrDocument() {
        return record -> {
            SolrInputDocument result = new SolrInputDocument();

            result.addField("id", String.valueOf(record.getRecordNumber()));

            final String name =  record.get("Hotel_Name").trim();
            if (name != null) result.addField("name_s", name);

            final String address = record.get("Hotel_Address").trim();
            if (address != null) result.addField("address_s", address);

            if (record.get("Average_Score") != null && record.get("Average_Score").length() != 0) {
                result.addField("average_score_f", Float.parseFloat(record.get("Average_Score")));
            }

            if (record.get("lat") != null && record.get("lng") != null) {
                try {
                    result.addField("loc_p", Double.parseDouble(record.get("lat"))+ "," + Double.parseDouble(record.get("lng")));
                } catch (Exception e) {
                    logger.info("No location point provided");
                }
            }

            final String reviewer_nationality = record.get("Reviewer_Nationality").trim();
            if (reviewer_nationality != null && reviewer_nationality.length() != 0)
                result.addField("reviewer_nationality_s", reviewer_nationality );

            final String positive_review = record.get("Positive_Review").trim();
            if (positive_review!= null && positive_review.length() != 0) {
                result.addField("positive_review_t", positive_review );
            }

            final String positive_review_word_counts = record.get("Review_Total_Positive_Word_Counts").trim();
            if (positive_review_word_counts != null ) {
                result.addField("positive_review_word_counts_i", Integer.valueOf(positive_review_word_counts));
            }

            final String negative_review = record.get("Negative_Review").trim();
            if (negative_review!= null && negative_review.length() != 0) {
                result.addField("negative_review_t", negative_review );
            }

            final String negative_review_word_counts = record.get("Review_Total_Negative_Word_Counts").trim();
            if (negative_review_word_counts != null ) {
                result.addField("negative_review_word_counts_i", Integer.valueOf(negative_review_word_counts));
            }

            final String reviewer_score = record.get("Reviewer_Score");
            if (reviewer_score != null && reviewer_score.length() != 0) {
                result.addField("reviewer_score_f", Float.parseFloat(reviewer_score));
            }

            final String tags = record.get("Tags").trim();
            if (tags != null && tags.length() != 0) {
                result.addField("tags_s", tags);
            }

            final String review_date = record.get("Review_Date");
            if (review_date != null && review_date.length() != 0) {
                result.addField("review_date_tdt", review_date);
            }
            return result;
        };
    }

    private static Consumer<SolrInputDocument> addToSolr(SolrClient solr, String collection) {
        return doc -> {
            try {
                UpdateResponse resp = solr.add(collection, doc);
                logger.info(doc.getFieldValue("id") + " > " + resp.getStatus());
            } catch (SolrServerException | IOException ex) {
                logger.error("Exception while indexing", ex);
                throw new RuntimeException("Exception while indexing", ex);
            }
        };
    }
}
