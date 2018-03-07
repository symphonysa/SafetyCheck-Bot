package bot;

import POJO.*;
import config.BotConfig;
import mongo.MongoDBClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.events.*;
import org.symphonyoss.client.exceptions.MessagesException;
import org.symphonyoss.client.exceptions.SymException;
import org.symphonyoss.client.exceptions.UsersClientException;
import org.symphonyoss.client.model.Chat;
import org.symphonyoss.client.model.Room;
import org.symphonyoss.client.services.RoomEventListener;
import org.symphonyoss.client.services.RoomService;
import org.symphonyoss.client.services.RoomServiceEventListener;
import org.symphonyoss.symphony.clients.model.SymMessage;
import org.symphonyoss.symphony.clients.model.SymUser;
import org.symphonyoss.symphony.pod.model.Stream;
import utils.MessageParser;
import utils.ReportGenerator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RoomChatBot implements RoomServiceEventListener, RoomEventListener {

    private static RoomChatBot instance;
    private final Logger logger = LoggerFactory.getLogger(RoomChatBot.class);
    private SymphonyClient symClient;
    private RoomService roomService;
    private BotConfig config;
    private MongoDBClient mongoDBClient;
    private MessageParser messageParser;


    protected RoomChatBot(SymphonyClient symClient, BotConfig config) {
        this.symClient=symClient;
        this.config = config;
        init();


    }

    public static RoomChatBot getInstance(SymphonyClient symClient, BotConfig config){
        if(instance==null){
            instance = new RoomChatBot(symClient,config);
        }
        return instance;
    }

    private void init() {

        roomService = symClient.getRoomService();
        roomService.addRoomServiceEventListener(this);
        mongoDBClient = MongoDBClient.init(config.getMongoURL());
        messageParser = new MessageParser();

    }

    @Override
    public void onRoomMessage(SymMessage message) {

    }

    @Override
    public void onNewRoom(Room room) {
        room.addEventListener(this);
    }

    @Override
    public void onMessage(SymMessage message) {
        if (message == null)
            return;
        logger.debug("TS: {}\nFrom ID: {}\nSymMessage: {}\nSymMessage Type: {}",
                message.getTimestamp(),
                message.getFromUserId(),
                message.getMessage(),
                message.getMessageType());

        SymMessage message2= new SymMessage();
        MessageEntities entities = messageParser.getMessageEntities(message.getEntityData());

        Incident activeIncident = mongoDBClient.getActiveIncidentInRoom(message.getStreamId());

        if (entities.getHashtags().contains("safetycheck")) {

            if (activeIncident==null){
                Incident incident = new Incident();
                incident.setTimestamp(message.getTimestamp());
                incident.setStreamId(message.getStreamId());
                incident.setActive(true);
                incident.setEmail(message.getSymUser().getEmailAddress());
                mongoDBClient.insertIncident(incident);

                message2.setMessage("<messageML><div>Safety check initiated. All room members please reply <hash tag=\"safe\"/>. To report that someone is safe @mention them along with <hash tag=\"safe\"/> in a single message in this room. To add a visitor, who is not in the room but should be accounted for @mention them along with <hash tag=\"visitor\"/> </div></messageML>");
                Chat chat = new Chat();
                chat.setLocalUser(symClient.getLocalUser());
                Set<SymUser> recipients = new HashSet<>();
                recipients.add(message.getSymUser());
                chat.setRemoteUsers(recipients);
                symClient.getChatService().addChat(chat);
                SymMessage reporterMessage = new SymMessage();
                reporterMessage.setMessage("<messageML><div>You started a safety check in "+symClient.getRoomService().getRoom(message.getStreamId()).getRoomDetail().getRoomAttributes().getName()+". Send <hash tag=\"report\"/> in this conversation to get a report or send <hash tag=\"endsafetycheck\"/> to the room to end the safety check and get a report.</div></messageML>");
                try {
                    symClient.getMessagesClient().sendMessage(chat.getStream(), reporterMessage);
                } catch (MessagesException e) {
                    logger.error("Failed to send message", e);
                }
            }
            else {
                message2.setMessage("<messageML><div>Safety check is ongoing. Send <hash tag=\"endsafetycheck\"/> to finalize it.</div></messageML>");
            }
            try {
                symClient.getMessagesClient().sendMessage(message.getStream(), message2);
            } catch (MessagesException e) {
                logger.error("Failed to send message", e);
            }

        }

        else if (entities.getHashtags().contains("endsafetycheck")) {

            if (activeIncident!=null){
                closeIncident(activeIncident);
            }
        }
        else if (entities.getHashtags().contains("safe")) {
            if (activeIncident!=null){
                MemberCheck memberCheck = new MemberCheck();
                memberCheck.setIncidentId(activeIncident.getId().toString());
                memberCheck.setSafe(true);
                memberCheck.setUserId(message.getSymUser().getId());
                memberCheck.setMessage(message.getMessageText());
                mongoDBClient.insertMemberCheck(memberCheck);
                for (String user: entities.getUsers()) {
                    MemberCheck memberCheckOBO = new MemberCheck();
                    memberCheckOBO.setIncidentId(activeIncident.getId().toString());
                    memberCheckOBO.setSafe(true);
                    memberCheckOBO.setUserId(Long.parseLong(user));
                    memberCheckOBO.setMessage(message.getSymUser().getDisplayName()+": "+message.getMessageText());
                    mongoDBClient.insertMemberCheck(memberCheckOBO);
                }
                try {
                    int safeNumber=mongoDBClient.getSafeMembers(activeIncident.getId().toString()).size();
                    int members = symClient.getRoomMembershipClient().getRoomMembership(message.getStreamId()).size();
                    int visitors = mongoDBClient.getVisitors(activeIncident.getId().toString()).size();
                    int totalMembers = members + visitors -1;
                    if(safeNumber == totalMembers){
                        closeIncident(activeIncident);
                    }
                } catch (SymException e) {
                    e.printStackTrace();
                }
            }
        }
        else if (entities.getHashtags().contains("visitor")) {
            if (activeIncident!=null) {
                for (String user : entities.getUsers()) {
                    IncidentVisitor visitor = new IncidentVisitor();
                    visitor.setIncidentId(activeIncident.getId().toString());
                    visitor.setUserId(user);
                    mongoDBClient.addVisitor(visitor);
                }
            }
        }

    }

    @Override
    public void onSymRoomDeactivated(SymRoomDeactivated symRoomDeactivated) {

    }

    @Override
    public void onSymRoomMemberDemotedFromOwner(SymRoomMemberDemotedFromOwner symRoomMemberDemotedFromOwner) {

    }

    @Override
    public void onSymRoomMemberPromotedToOwner(SymRoomMemberPromotedToOwner symRoomMemberPromotedToOwner) {

    }

    @Override
    public void onSymRoomReactivated(SymRoomReactivated symRoomReactivated) {

    }

    @Override
    public void onSymRoomUpdated(SymRoomUpdated symRoomUpdated) {

    }

    @Override
    public void onSymUserJoinedRoom(SymUserJoinedRoom symUserJoinedRoom) {

    }

    @Override
    public void onSymUserLeftRoom(SymUserLeftRoom symUserLeftRoom) {

    }

    @Override
    public void onSymRoomCreated(SymRoomCreated symRoomCreated) {

    }

    public void closeIncident(Incident activeIncident){
        SymMessage roomMessage = new SymMessage();
        mongoDBClient.closeIncident(activeIncident);
        ReportGenerator reportGenerator = new ReportGenerator(symClient, config);
        List<RoomMember> report = reportGenerator.generate(activeIncident.getId().toString(), activeIncident.getStreamId());


        roomMessage.setMessage("<messageML><div>Safety check finalized.</div></messageML>");
        Stream roomStream = new Stream();
        roomStream.setId(activeIncident.getStreamId());
        try {
            symClient.getMessagesClient().sendMessage(roomStream, roomMessage);
        } catch (MessagesException e) {
            logger.error("Failed to send message", e);
        }

        Chat chat = new Chat();
        chat.setLocalUser(symClient.getLocalUser());
        Set<SymUser> recipients = new HashSet<>();
        try {
            recipients.add(symClient.getUsersClient().getUserFromEmail(activeIncident.getEmail()));
        } catch (UsersClientException e) {
            e.printStackTrace();
        }
        chat.setRemoteUsers(recipients);
        symClient.getChatService().addChat(chat);
        SymMessage reporterMessage = new SymMessage();
        reporterMessage.setMessage(reportGenerator.formatTable(report));
        try {
            symClient.getMessagesClient().sendMessage(chat.getStream(),reporterMessage);
        } catch (MessagesException e) {
            logger.error("Failed to send message", e);
        }
    }
}
