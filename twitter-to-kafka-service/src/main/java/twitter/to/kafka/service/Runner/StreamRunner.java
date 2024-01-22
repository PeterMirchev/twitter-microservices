package twitter.to.kafka.service.Runner;

import twitter4j.TwitterException;

public interface StreamRunner {
    void start() throws TwitterException;
}
