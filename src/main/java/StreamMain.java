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
            result.addField("address_s", record.get("Hotel_Address").trim());
            result.addField("name_s", record.get("Hotel_Name").trim());
            result.addField("averageScore_f", Float.parseFloat(record.get("Average_Score")));
            result.addField("reviewerNationality_s", record.get("Reviewer_Nationality").trim());
            result.addField("positiveReview_s", record.get("Positive_Review").trim());
            result.addField("negativeReview_s", record.get("Negative_Review").trim());
            result.addField("reviewerScore_f", Float.parseFloat(record.get("Reviewer_Score")));
            result.addField("tags_s", record.get("Tags").trim());
            if (record.get("lat") != null && record.get("lng") != null) {
                try {
                    result.addField("loc_p", Double.parseDouble(record.get("lat"))+ "," + Double.parseDouble(record.get("lng")));
                } catch (Exception e) {
                    logger.info("No location point provided");
                }

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
