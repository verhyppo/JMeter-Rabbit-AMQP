package com.zeroclue.jmeter.protocol.amqp.gui;

import com.zeroclue.jmeter.protocol.amqp.AMQPPublisher;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.config.gui.ArgumentsPanel;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jorphan.gui.JLabeledTextArea;
import org.apache.jorphan.gui.JLabeledTextField;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import java.awt.Dimension;

/**
 * AMQP Sampler
 * <p>
 * This class is responsible for ensuring that the Sampler data is kept in step
 * with the GUI.
 * <p>
 * The GUI class is not invoked in non-GUI mode, so it should not perform any
 * additional setup that a test would need at run-time
 */
public class AMQPPublisherGui extends AMQPSamplerGui {

    private static final long serialVersionUID = 1L;
    private final JLabeledTextArea message = new JLabeledTextArea("Message Content");
    private final JLabeledTextField messageRoutingKey = new JLabeledTextField("Routing Key");
    private final JLabeledTextField messageType = new JLabeledTextField("Message Type");
    private final JLabeledTextField replyToQueue = new JLabeledTextField("Reply-To Queue");
    private final JLabeledTextField correlationId = new JLabeledTextField("Correlation Id");
    private final JLabeledTextField contentType = new JLabeledTextField("ContentType");
    private final JLabeledTextField messageId = new JLabeledTextField("Message Id");
    private final JCheckBox persistent = new JCheckBox("Persistent?", AMQPPublisher.DEFAULT_PERSISTENT);
    private final JCheckBox useTx = new JCheckBox("Use Transactions?", AMQPPublisher.DEFAULT_USE_TX);
    private final ArgumentsPanel headers = new ArgumentsPanel("Headers");
    private JPanel mainPanel;

    public AMQPPublisherGui() {
        init();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLabelResource() {
        return this.getClass().getSimpleName();
    }

    @Override
    public String getStaticLabel() {
        return "AMQP Publisher";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(TestElement element) {
        super.configure(element);
        if (!(element instanceof AMQPPublisher)) return;
        AMQPPublisher sampler = (AMQPPublisher) element;

        persistent.setSelected(sampler.getPersistent());
        useTx.setSelected(sampler.getUseTx());

        messageRoutingKey.setText(sampler.getMessageRoutingKey());
        messageType.setText(sampler.getMessageType());
        replyToQueue.setText(sampler.getReplyToQueue());
        contentType.setText(sampler.getContentType());
        correlationId.setText(sampler.getCorrelationId());
        messageId.setText(sampler.getMessageId());
        message.setText(sampler.getMessage());
        configureHeaders(sampler);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TestElement createTestElement() {
        AMQPPublisher sampler = new AMQPPublisher();
        modifyTestElement(sampler);
        return sampler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void modifyTestElement(TestElement te) {
        AMQPPublisher sampler = (AMQPPublisher) te;
        sampler.clear();
        configureTestElement(sampler);

        super.modifyTestElement(sampler);

        sampler.setPersistent(persistent.isSelected());
        sampler.setUseTx(useTx.isSelected());

        sampler.setMessageRoutingKey(messageRoutingKey.getText());
        sampler.setMessage(message.getText());
        sampler.setMessageType(messageType.getText());
        sampler.setReplyToQueue(replyToQueue.getText());
        sampler.setCorrelationId(correlationId.getText());
        sampler.setContentType(contentType.getText());
        sampler.setMessageId(messageId.getText());
        sampler.setHeaders((Arguments) headers.createTestElement());
    }

    @Override
    protected void setMainPanel(JPanel panel) {
        mainPanel = panel;
    }

    /*
     * Helper method to set up the GUI screen
     */
    @Override
    protected final void init() {
        super.init();
        persistent.setPreferredSize(new Dimension(100, 25));
        useTx.setPreferredSize(new Dimension(100, 25));
        messageRoutingKey.setPreferredSize(new Dimension(100, 25));
        messageType.setPreferredSize(new Dimension(100, 25));
        replyToQueue.setPreferredSize(new Dimension(100, 25));
        correlationId.setPreferredSize(new Dimension(100, 25));
        contentType.setPreferredSize(new Dimension(100, 25));
        messageId.setPreferredSize(new Dimension(100, 25));
        message.setPreferredSize(new Dimension(400, 150));

        mainPanel.add(persistent);
        mainPanel.add(useTx);
        mainPanel.add(messageRoutingKey);
        mainPanel.add(messageType);
        mainPanel.add(replyToQueue);
        mainPanel.add(correlationId);
        mainPanel.add(contentType);
        mainPanel.add(messageId);
        mainPanel.add(headers);
        mainPanel.add(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearGui() {
        super.clearGui();
        persistent.setSelected(AMQPPublisher.DEFAULT_PERSISTENT);
        useTx.setSelected(AMQPPublisher.DEFAULT_USE_TX);
        messageRoutingKey.setText("");
        messageType.setText("");
        replyToQueue.setText("");
        correlationId.setText("");
        contentType.setText("");
        messageId.setText("");
        headers.clearGui();
        message.setText("");
    }

    private void configureHeaders(AMQPPublisher sampler) {
        Arguments sampleHeaders = sampler.getHeaders();
        if (sampleHeaders != null) {
            headers.configure(sampleHeaders);
        } else {
            headers.clearGui();
        }
    }
}
