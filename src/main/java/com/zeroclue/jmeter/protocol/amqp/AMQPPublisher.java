package com.zeroclue.jmeter.protocol.amqp;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.property.TestElementProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * JMeter creates an instance of a sampler class for every occurrence of the
 * element in every thread. [some additional copies may be created before the
 * test run starts]
 * <p>
 * Thus each sampler is guaranteed to be called by a single thread - there is no
 * need to synchronize access to instance variables.
 * <p>
 * However, access to class fields must be synchronized.
 */
public class AMQPPublisher extends AMQPSampler implements Interruptible {

    public static final boolean DEFAULT_PERSISTENT = false;
    public static final boolean DEFAULT_USE_TX = false;
    private static final long serialVersionUID = -8420658040465788497L;
    private static final Logger log = LoggerFactory.getLogger(AMQPPublisher.class);
    //++ These are JMX names, and must not be changed
    private static final String MESSAGE = "AMQPPublisher.Message";
    private static final String MESSAGE_ROUTING_KEY = "AMQPPublisher.MessageRoutingKey";
    private static final String MESSAGE_TYPE = "AMQPPublisher.MessageType";
    private static final String REPLY_TO_QUEUE = "AMQPPublisher.ReplyToQueue";
    private static final String CONTENT_TYPE = "AMQPPublisher.ContentType";
    private static final String CORRELATION_ID = "AMQPPublisher.CorrelationId";
    private static final String MESSAGE_ID = "AMQPPublisher.MessageId";
    private static final String HEADERS = "AMQPPublisher.Headers";
    private static final String PERSISTENT = "AMQPPublisher.Persistent";
    private static final String USE_TX = "AMQPPublisher.UseTx";

    private transient Channel channel;

    public AMQPPublisher() {
        super();
    }

    /**
     * constructor for testing purposes
     *
     * @param factory connection factory
     */
    AMQPPublisher(ConnectionFactory factory) {
        super(factory);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public SampleResult sample(Entry e) {
        SampleResult result = new SampleResult();
        result.setSampleLabel(getName());
        result.setSuccessful(false);
        result.setResponseCode("500");

        try {
            initChannel();
        } catch (Exception ex) {
            log.error("Failed to initialize channel : ", ex);
            result.setResponseMessage(ex.toString());
            return result;
        }

        result.setSampleLabel(getTitle());
        /*
         * Perform the sampling
         */

        // aggregate samples.
        int loop = getIterationsAsInt();
        result.sampleStart(); // Start timing
        try {
            AMQP.BasicProperties messageProperties = getProperties();
            byte[] messageBytes = getMessageBytes();

            for (int idx = 0; idx < loop; idx++) {
                // try to force jms semantics.
                // but this does not work since RabbitMQ does not sync to disk if consumers are connected as
                // seen by iostat -cd 1. TPS value remains at 0.
                result.addSubResult(sample("Sample " + idx, messageProperties, messageBytes));
            }

            // commit the sample.
            if (getUseTx()) {
                channel.txCommit();
            }

            /*
             * Set up the sample result details
             */
            result.setSamplerData("see sub sample for details...");
            result.setDataType(SampleResult.TEXT);

            result.setResponseCodeOK();
            result.setResponseMessage("OK");
            result.setSampleCount(loop);
            result.setSuccessful(Arrays.stream(result.getSubResults())
                                       .allMatch(SampleResult::isSuccessful));
        } catch (Exception ex) {
            log.error("Exception during execution", ex);
            result.setResponseCode("000");
            result.setResponseMessage(ex.getMessage());
        } finally {
            result.sampleEnd(); // End timimg
        }

        return result;
    }

    private SampleResult sample(String label, AMQP.BasicProperties messageProperties, byte[] messageBytes) {
        SampleResult subSample = new SampleResult();
        subSample.setSampleLabel(label);
        subSample.sampleStart();
        try {
            log.info("publish: {}, {}", getExchange(), getMessageRoutingKey());
            channel.basicPublish(getExchange(), getMessageRoutingKey(), messageProperties, messageBytes);
            subSample.setSamplerData(label + "published ");
            subSample.setResponseOK();
        } catch (IOException ex) {
            subSample.setSamplerData(ex.getMessage());
        }
        return subSample;
    }


    private byte[] getMessageBytes() {
        return getMessage().getBytes();
    }

    /**
     * @return the message routing key for the sample
     */
    public String getMessageRoutingKey() {
        return getPropertyAsString(MESSAGE_ROUTING_KEY);
    }

    public void setMessageRoutingKey(String content) {
        setProperty(MESSAGE_ROUTING_KEY, content);
    }

    /**
     * @return the message for the sample
     */
    public String getMessage() {
        return getPropertyAsString(MESSAGE);
    }

    public void setMessage(String content) {
        setProperty(MESSAGE, content);
    }

    /**
     * @return the message type for the sample
     */
    public String getMessageType() {
        return getPropertyAsString(MESSAGE_TYPE);
    }

    public void setMessageType(String content) {
        setProperty(MESSAGE_TYPE, content);
    }

    /**
     * @return the reply-to queue for the sample
     */
    public String getReplyToQueue() {
        return getPropertyAsString(REPLY_TO_QUEUE);
    }

    public void setReplyToQueue(String content) {
        setProperty(REPLY_TO_QUEUE, content);
    }

    public String getContentType() {
        return getPropertyAsString(CONTENT_TYPE);
    }

    public void setContentType(String contentType) {
        setProperty(CONTENT_TYPE, contentType);
    }

    /**
     * @return the correlation identifier for the sample
     */
    public String getCorrelationId() {
        return getPropertyAsString(CORRELATION_ID);
    }

    public void setCorrelationId(String content) {
        setProperty(CORRELATION_ID, content);
    }

    /**
     * @return the message id for the sample
     */
    public String getMessageId() {
        return getPropertyAsString(MESSAGE_ID);
    }

    public void setMessageId(String content) {
        setProperty(MESSAGE_ID, content);
    }

    public Arguments getHeaders() {
        return (Arguments) getProperty(HEADERS).getObjectValue();
    }

    public void setHeaders(Arguments headers) {
        setProperty(new TestElementProperty(HEADERS, headers));
    }

    public boolean getPersistent() {
        return getPropertyAsBoolean(PERSISTENT, DEFAULT_PERSISTENT);
    }

    public void setPersistent(Boolean persistent) {
        setProperty(PERSISTENT, persistent);
    }

    public boolean getUseTx() {
        return getPropertyAsBoolean(USE_TX, DEFAULT_USE_TX);
    }

    public void setUseTx(Boolean tx) {
        setProperty(USE_TX, tx);
    }

    @Override
    public boolean interrupt() {
        cleanup();
        return true;
    }

    @Override
    protected Channel getChannel() {
        return channel;
    }

    @Override
    protected void setChannel(Channel channel) {
        this.channel = channel;
    }

    protected AMQP.BasicProperties getProperties() {
        final AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();

        final int deliveryMode = getPersistent() ? 2 : 1;
        final String contentType = StringUtils.defaultIfEmpty(getContentType(), "text/plain");

        builder.contentType(contentType)
               .deliveryMode(deliveryMode)
               .priority(0)
               .correlationId(getCorrelationId())
               .replyTo(getReplyToQueue())
               .type(getMessageType())
               .headers(prepareHeaders())
               .build();
        if (getMessageId() != null && !getMessageId().isEmpty()) {
            builder.messageId(getMessageId());
        }
        return builder.build();
    }

    @Override
    protected boolean initChannel() throws IOException, NoSuchAlgorithmException, KeyManagementException, TimeoutException {
        boolean ret = super.initChannel();
        if (getUseTx()) {
            channel.txSelect();
        }
        return ret;
    }

    private Map<String, Object> prepareHeaders() {
        Arguments headers = getHeaders();
        if (headers != null) {
            return new HashMap<>(headers.getArgumentsAsMap());
        }
        return Collections.emptyMap();
    }
}
