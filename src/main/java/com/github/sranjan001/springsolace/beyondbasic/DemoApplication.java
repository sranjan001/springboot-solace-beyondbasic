package com.github.sranjan001.springsolace.beyondbasic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SpringBootApplication
public class DemoApplication {
    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    //Consumer function that receives a String. No Header
    @Bean
    public Consumer<String> myConsumer() {
        return v -> {
            logger.info("Received: " + v);
        };
    }

    //Publisher
    @Bean
    Supplier<Message<String>> mySupplier(){
        return () -> {
            System.out.println("Inside my supplier");
            return MessageBuilder.withPayload("Hello Headers").setHeader("Key", "Value").build();
        };
    }

    /***** Consumer function that reads message header  *****/
   /* @Bean
    public Consumer<Message<String>> myConsumer() {
        return v -> {
            logger.info("Received: " + v.getPayload());
            logger.info("All Headers: " + v.getHeaders());
            logger.info("TTL: " + v.getHeaders().get("solace_timeToLive"));
            logger.info("My Customer Header: " + v.getHeaders().get("Key"));
        };
    }*/

    /* StreamBridge Dynamic Publishing
    @Bean
    public Consumer<Message<String>> myConsumer(StreamBridge sb) {
        return v -> {
            logger.info("Received myConsumer: " + v.getPayload());
            logger.info("CorrelationID: " + v.getHeaders().get("solace_correlationId"));

            // Use whatever business logic you'd like to figure out the topic!
            String cid = (String) v.getHeaders().get("solace_correlationId");
            if (cid == null) {
                cid = Math.random() + "";
            }
            String myTopic = "solace/cid/".concat(cid);
            logger.info("Publishing to: " + myTopic);
            sb.send(myTopic, v.getPayload());
        };
    }*/


    /****** Dynamic Publishing - BinderHeader.TARGET_DESTINATION ********/
    /*@Bean
    public Function<Message<String>, Message<String>> myFunction() {
        return v -> {
            logger.info("Received myFunction: " + v.getPayload());
            logger.info("CorrelationID: " + v.getHeaders().get("solace_correlationId"));

            // Use whatever business logic you'd like to figure out the topic!
            String cid = (String) v.getHeaders().get("solace_correlationId");
            if (cid == null) {
                cid = Math.random() + "";
            }
            String myTopic = "solace/cid/".concat(cid);
            logger.info("Publishing to: " + myTopic);
            return MessageBuilder.withPayload(v.getPayload()).setHeader(BinderHeaders.TARGET_DESTINATION, myTopic).build();
        };
    }
    */

    /*********** Batch publishing to default binding destination *********/
    /*
    @Bean
    public Function<String, Collection<Message<String>>> myFunction() {
        return v -> {
            logger.info("Received: " + v);

            ArrayList<Message<String>> msgList = new ArrayList<Message<String>>();
            msgList.add(MessageBuilder.withPayload("Payload 1").build());
            msgList.add(MessageBuilder.withPayload("Payload 2").build());
            msgList.add(MessageBuilder.withPayload("Payload 3").build());

            return msgList;
        };
    }*/

    /**** Batch Publish to Dynamic Binding Destinations  ****/
    /*@Bean
    public Function<String, Collection<Message<String>>> myFunction(StreamBridge sb) {
        return v -> {
            logger.info("Received: " + v);

            // Do some processing & use StreamBridge to send an Alert to a dynamic topic
            sb.send("some/other/topic/1", v);

            // Do some more processing and create a list of messages to send upon returning
            ArrayList<Message<String>> msgList = new ArrayList<Message<String>>();
            // Send to default topic
            msgList.add(MessageBuilder.withPayload("Payload 1").build());
            // Send to dynamic topics using BinderHeaders.TARGET_DESTINATION
            msgList.add(MessageBuilder.withPayload("Payload 2").setHeader(BinderHeaders.TARGET_DESTINATION, "some/other/topic/2").build());
            msgList.add(MessageBuilder.withPayload("Payload 3").setHeader(BinderHeaders.TARGET_DESTINATION, "some/other/topic/3").build());

            return msgList;
        };
    }*/

    /******* Client/Manual Acknowledgement  ******/
    /*
    @Bean
    public Function<Message<String>, String> myFunction() {
        return v -> {
            logger.info("Received: " + v);

            // Disable Auto-Ack
            AcknowledgmentCallback ackCallback = StaticMessageHeaderAccessor.getAcknowledgmentCallback(v);
            ackCallback.noAutoAck();

            // TODO Execute Business Logic + Maybe even pass to another thread?
            // Use CorrelationID for easy business logic...
            String cid = (String) v.getHeaders().get("solace_correlationId");
            if (cid == null) {
                cid = "none";
            }

            // Acknowledge the Message!
            try {
                if (cid.equals("accept")) {
                    logger.info("Accepting the Message");
                    AckUtils.accept(ackCallback);
                } else if (cid.equals("requeue")) {
                    logger.info("Requeuing the Message");
                    AckUtils.requeue(ackCallback);
                    Thread.sleep(60000);
                } else {
                    logger.info("Rejecting the Message");
                    AckUtils.reject(ackCallback);
                    Thread.sleep(60000);
                }
            } catch (SolaceAcknowledgmentException e) {
                logger.warn("Warning, exception occurred but message will be re-queued on broker and re-delivered", e);
                return null; //Don't send an output message
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return "My Payload";
        };
    }*/

    /********* Listening to binding specific error channel  <destination>.<group>.errors *********/
    /*
    @ServiceActivator(inputChannel = "a/b/>.clientAck.errors")
    public void handleError(ErrorMessage message) {
        logger.info("Binding Specific Error Handler executing business logic for: " + message.toString());
        logger.info("Exception is here: " + message.getPayload());
    }

    //Listening to global error channel. Only receive the ErrorMessage if there is no binding specific error channel
    @ServiceActivator(inputChannel = "errorChannel")
    public void handleNotificationErrorChannel(ErrorMessage message) {
        logger.info("Global errorChannel received msg. NO BUSINESS LOGIC HERE! Notify ONLY!" + message.toString());
    }
   */

    /******* Consumer Error handling  ********/
    /*
    @Bean
    public Function<Message<String>, String> myFunction() {
        return v -> {
            logger.info("Received: " + v);

            // Logic to Avoid infinite loop of message being re-delivered when testing error
            // handling during codelab. DO NOT USE IN PRODUCTION CODE
            if (true == (Boolean) v.getHeaders().get("solace_redelivered")) {
                logger.warn("Exiting successfully to ACK msg and avoid infinite redelivieres");
                return null;
            }

            throw new RuntimeException("Oh no!");
        };
    } */

    /* Just return null if you don't want to send message after processing
    @Bean
    public Function<String, String> myFunction() {
        return v -> {
            logger.info("Received: " + v);

            if (!sendMessageDownstream(v)) {
                logger.warn("Not Sending an Outbound Message");
                return null; //Don't send a message, but ACCEPT it to remove it from the queue
            } else {
                return processMessage(v);
            }
        };
    }*/

    /*********************** Producer Error Handling *******************/

    /* Producer Error Channels (set error-channel-enabled: true in config) */
    /*
    @Bean
    public Function<Message<String>, String> myFunction() {
        return v -> {
            logger.info("Received: " + v);

            return v.getPayload().toUpperCase();
        };
    }

    @ServiceActivator(inputChannel="my/default/topic.errors")
    public void handlePublishError(ErrorMessage message) {
        logger.warn("Message Publish Failed for: " + message);
        logger.info("Original Message: " + message.getOriginalMessage());
    }*/


    /** Publisher Confirmation (This is binder specific) **/
    /*
    @Bean
    public Consumer<String> myConsumer(StreamBridge sb) {
        return v -> {
            logger.info("Received myConsumer: " + v);

            CorrelationData correlationData = new CorrelationData();
            Message<String> message = MessageBuilder.withPayload("My Payload")
                    .setHeader(SolaceBinderHeaders.CONFIRM_CORRELATION, correlationData).build();

            if (v.equals("fail")) {
                sb.send("my/default/topic", message);
            } else {
                sb.send("pub/sub/plus", message);
            }

            try {
                correlationData.getFuture().get(30, TimeUnit.SECONDS);
                logger.info("Publish Successful");
                // Do success logic
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error("Publish Failed");
                // Do failure logic
            }
        };
    } */

}
