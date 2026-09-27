package com.hackathon.platform.plagiarism;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlagiarismPropertiesTest {

  @Test
  void defaults_matchDocumentedValues() {

    PlagiarismProperties props = new PlagiarismProperties();

    assertThat(props.getDefaultTopN()).isEqualTo(20);
    assertThat(props.getKgramSize()).isEqualTo(15);
    assertThat(props.getWindowSize()).isEqualTo(6);
    assertThat(props.getFlagThreshold()).isEqualTo(0.6);
    assertThat(props.getMinTokenCount()).isEqualTo(20);
    assertThat(props.getStructuralWeight()).isEqualTo(0.6);
    assertThat(props.getEmbeddingWeight()).isEqualTo(0.4);
    assertThat(props.isUseRelativeThreshold()).isTrue();
    assertThat(props.getRelativeThresholdZScore()).isEqualTo(2.0);
    assertThat(props.getMinPairsForRelativeThreshold()).isEqualTo(5);
    assertThat(props.getFunctionMatchThreshold()).isEqualTo(0.75);
    assertThat(props.getMaxFunctionMatches()).isEqualTo(15);
    assertThat(props.getQueue()).isNotNull();
  }

  @Test
  void settersAndGetters_roundTrip() {

    PlagiarismProperties props = new PlagiarismProperties();

    props.setDefaultTopN(5);
    props.setKgramSize(10);
    props.setWindowSize(3);
    props.setFlagThreshold(0.9);
    props.setMinTokenCount(50);
    props.setStructuralWeight(0.7);
    props.setEmbeddingWeight(0.3);
    props.setUseRelativeThreshold(false);
    props.setRelativeThresholdZScore(1.5);
    props.setMinPairsForRelativeThreshold(3);
    props.setFunctionMatchThreshold(0.8);
    props.setMaxFunctionMatches(25);

    assertThat(props.getDefaultTopN()).isEqualTo(5);
    assertThat(props.getKgramSize()).isEqualTo(10);
    assertThat(props.getWindowSize()).isEqualTo(3);
    assertThat(props.getFlagThreshold()).isEqualTo(0.9);
    assertThat(props.getMinTokenCount()).isEqualTo(50);
    assertThat(props.getStructuralWeight()).isEqualTo(0.7);
    assertThat(props.getEmbeddingWeight()).isEqualTo(0.3);
    assertThat(props.isUseRelativeThreshold()).isFalse();
    assertThat(props.getRelativeThresholdZScore()).isEqualTo(1.5);
    assertThat(props.getMinPairsForRelativeThreshold()).isEqualTo(3);
    assertThat(props.getFunctionMatchThreshold()).isEqualTo(0.8);
    assertThat(props.getMaxFunctionMatches()).isEqualTo(25);
  }

  @Test
  void equalsAndHashCode_dependOnFieldValues() {

    PlagiarismProperties a = new PlagiarismProperties();
    PlagiarismProperties b = new PlagiarismProperties();

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());

    b.setKgramSize(99);

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void queue_defaultsMatchDocumentedValues() {

    PlagiarismProperties.Queue queue = new PlagiarismProperties.Queue();

    assertThat(queue.getStreamKey()).isEqualTo("plagiarism:jobs");
    assertThat(queue.getConsumerKey()).isEqualTo("plagiarism-workers");
    assertThat(queue.getConcurrency()).isEqualTo(2);
    assertThat(queue.getPollTimeoutMs()).isEqualTo(2000L);
  }

  @Test
  void queue_settersAndGetters_roundTrip() {

    PlagiarismProperties.Queue queue = new PlagiarismProperties.Queue();

    queue.setStreamKey("custom:stream");
    queue.setConsumerKey("custom-workers");
    queue.setConcurrency(8);
    queue.setPollTimeoutMs(5000L);

    assertThat(queue.getStreamKey()).isEqualTo("custom:stream");
    assertThat(queue.getConsumerKey()).isEqualTo("custom-workers");
    assertThat(queue.getConcurrency()).isEqualTo(8);
    assertThat(queue.getPollTimeoutMs()).isEqualTo(5000L);
  }

  @Test
  void setQueue_replacesQueueInstance() {

    PlagiarismProperties props = new PlagiarismProperties();
    PlagiarismProperties.Queue newQueue = new PlagiarismProperties.Queue();
    newQueue.setStreamKey("replaced:stream");

    props.setQueue(newQueue);

    assertThat(props.getQueue().getStreamKey()).isEqualTo("replaced:stream");
  }
}
