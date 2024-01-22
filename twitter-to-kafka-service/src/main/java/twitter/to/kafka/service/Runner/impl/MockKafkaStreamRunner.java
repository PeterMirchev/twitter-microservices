package twitter.to.kafka.service.Runner.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import twitter.to.kafka.service.Runner.StreamRunner;
import twitter.to.kafka.service.config.TwitterToKafkaServiceConfigData;
import twitter.to.kafka.service.exception.TwitterToKafkaServiceException;
import twitter.to.kafka.service.listener.TwitterKafkaStatusListener;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-mock-tweets", havingValue = "true")
public class MockKafkaStreamRunner implements StreamRunner {

    private static final Logger LOG = LoggerFactory.getLogger(MockKafkaStreamRunner.class);

    private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;

    private final TwitterKafkaStatusListener twitterKafkaStatusListener;

    private static final Random RANDOM = new Random();

    private static final String[] WORDS = new String[] {
            "amet",
            "porttitor",
            "eget",
            "dolor",
            "morbi",
            "non",
            "arcu",
            "risus",
            "quis",
            "varius",
            "quam",
            "quisque" ,
            "id diam",
           " vel quam",
            "elementum",
            "pulvinar",
           " etiam non",
    };

    private static final String tweetAsRawJson = "{" +
            "\"created_at\":\"{0}\"," +
            "\"id\":\"{1}\"," +
            "\"text\":\"{2}\"," +
            "\"user\":{\"id\": {3}\"}" +
            "}";

    private static final String TWITTER_STATUS_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";
    public MockKafkaStreamRunner(TwitterToKafkaServiceConfigData configData,
                                 TwitterKafkaStatusListener statusListener) {
        this.twitterToKafkaServiceConfigData = configData;
        this.twitterKafkaStatusListener = statusListener;
    }

    @Override
    public void start() throws TwitterException {

        String[] keywords = twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[0]);

        int minTweetLength = twitterToKafkaServiceConfigData.getMockMinTweetLength();
        int maxTweetLength = twitterToKafkaServiceConfigData.getMockMaxTweetLength();
        Long sleepTimeMs = twitterToKafkaServiceConfigData.getMockSleepMs();

        LOG.info("Starting mock filtering twitter streams for keywods {}", Arrays.toString(keywords));

        simulateTweeterStream(keywords, minTweetLength, maxTweetLength, sleepTimeMs);

    }

    private void simulateTweeterStream(String[] keywords,
                                       int minTweetLength,
                                       int maxTweetLength,
                                       Long sleepTimeMs) {

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                while (true) {
                    String formattedTweetAsRawJson = getFormattedTweet(keywords, minTweetLength, maxTweetLength);
                    Status status = TwitterObjectFactory.createStatus(formattedTweetAsRawJson);
                    twitterKafkaStatusListener.onStatus(status);
                    sleep(sleepTimeMs);
                }
            } catch (TwitterException e) {
                LOG.error("Error creating twitter status!", e);
            }
        });


    }

    private void sleep(Long sleepTimeMs) {
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            throw new TwitterToKafkaServiceException("Error while sleeping for waiting new status to create!!");
        }
    }

    private String getFormattedTweet(String[] keywords,
                                     int minTweetLength,
                                     int maxTweetLength) {

        String[] params = new String[] {
                ZonedDateTime.now().format(DateTimeFormatter.ofPattern(TWITTER_STATUS_DATE_FORMAT, Locale.ENGLISH)),
                String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)),
                getRandomTweetContent(keywords, minTweetLength, maxTweetLength),
                String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)),
        };

        return formatTwitterAsJsonWithParams(params);
    }

    private static String formatTwitterAsJsonWithParams(String[] params) {

        String tweet = tweetAsRawJson;
        for (int i = 0; i < params.length; i++) {
            tweet = tweet.replace("{" + i + "}", params[i]);
        }

        return tweet;
    }

    private String getRandomTweetContent(String[] keywords,
                                         int minTweetLength,
                                         int maxTweetLength) {

        StringBuilder tweet = new StringBuilder();

        int tweetLength = RANDOM.nextInt(maxTweetLength - minTweetLength + 1) + minTweetLength;

        return constructRandomTweet(keywords, tweet, tweetLength);
    }

    private static String constructRandomTweet(String[] keywords, StringBuilder tweet, int tweetLength) {

        for (int i = 0; i < tweetLength; i++) {

            tweet.append(keywords[RANDOM.nextInt(keywords.length)]).append(" ");

            if (i == tweetLength / 2) {
                tweet.append(keywords[RANDOM.nextInt(keywords.length)]).append(" ");
            }

        }

        return tweet.toString().trim();
    }
}
