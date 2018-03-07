package bot;

import POJO.Incident;
import POJO.RoomMember;
import config.BotConfig;
import mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.exceptions.MessagesException;
import org.symphonyoss.client.model.Chat;
import org.symphonyoss.client.services.*;
import org.symphonyoss.symphony.clients.model.SymMessage;
import utils.ReportGenerator;

import java.util.List;

public class ChatBot implements ChatListener, ChatServiceListener {

    private static ChatBot instance;
    private final Logger logger = LoggerFactory.getLogger(ChatBot.class);
    private SymphonyClient symClient;
    private BotConfig config;
    private MongoDBClient mongoDBClient;


    protected ChatBot(SymphonyClient symClient, BotConfig config) {
        this.symClient=symClient;
        this.config = config;
        init();


    }

    public static ChatBot getInstance(SymphonyClient symClient, BotConfig config){
        if(instance==null){
            instance = new ChatBot(symClient,config);
        }
        return instance;
    }

    private void init() {


        symClient.getChatService().addListener(this);
        mongoDBClient = MongoDBClient.init(config.getMongoURL());

    }


    public void onChatMessage(SymMessage message) {
        if (message == null)
            return;
        logger.debug("TS: {}\nFrom ID: {}\nSymMessage: {}\nSymMessage Type: {}",
                message.getTimestamp(),
                message.getFromUserId(),
                message.getMessage(),
                message.getMessageType());
        SymMessage message2= new SymMessage();

        if (message.getMessageText().toLowerCase().contains("#report") ) {

            Incident incident = mongoDBClient.getReportedIncident(message.getSymUser().getEmailAddress());

            if(incident!=null) {
                ReportGenerator reportGenerator = new ReportGenerator(symClient, config);
                List<RoomMember> report = reportGenerator.generate(incident.getId().toString(), incident.getStreamId());


                message2.setMessage(reportGenerator.formatTable(report));

            }
            else {
                message2.setMessage("<messageML><div>No active incident found</div></messageML>");
            }
            try {
                symClient.getMessagesClient().sendMessage(message.getStream(), message2);
            } catch (MessagesException e) {
                logger.error("Failed to send message", e);
            }
        } else if (message.getMessageText().toLowerCase().contains("#help") | message.getMessageText().toLowerCase().contains("/help") ) {

            message2.setMessage("<messageML><div>I'm SafetyCheck Bot and my purpose is to account for all members of a room in case their safety is in question. To start a safety check, add me to a room and send <hash tag=\"startsafetycheck\"/>. You can request status of a safety check by sending <hash tag=\"report\"/> in this 1-1 conversation. </div></messageML>");
            try {
                symClient.getMessagesClient().sendMessage(message.getStream(), message2);
            } catch (MessagesException e) {
                logger.error("Failed to send message", e);
            }
        }


    }



    @Override
    public void onNewChat(Chat chat) {

        chat.addListener(this);

        logger.debug("New chat session detected on stream {} with {}", chat.getStream().getStreamId(), chat.getRemoteUsers());
    }

    @Override
    public void onRemovedChat(Chat chat) {

    }


}
