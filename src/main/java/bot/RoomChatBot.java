package bot;

import config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.events.*;
import org.symphonyoss.client.exceptions.MessagesException;
import org.symphonyoss.client.model.Room;
import org.symphonyoss.client.services.RoomEventListener;
import org.symphonyoss.client.services.RoomService;
import org.symphonyoss.client.services.RoomServiceEventListener;
import org.symphonyoss.symphony.clients.model.SymMessage;

public class RoomChatBot implements RoomServiceEventListener, RoomEventListener {

    private static RoomChatBot instance;
    private final Logger logger = LoggerFactory.getLogger(RoomChatBot.class);
    private SymphonyClient symClient;
    private RoomService roomService;
    private BotConfig config;


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


    }

    @Override
    public void onRoomMessage(SymMessage message) {

        if (message == null)
            return;
        logger.debug("TS: {}\nFrom ID: {}\nSymMessage: {}\nSymMessage Type: {}",
                message.getTimestamp(),
                message.getFromUserId(),
                message.getMessage(),
                message.getMessageType());
        SymMessage message2;

        if (message.getMessageText().toLowerCase().contains("test")) {


            message2 = new SymMessage();

            message2.setMessage("<messageML><div><b><i>Message Received.</i></b></div></messageML>");
            try {
                symClient.getMessagesClient().sendMessage(message.getStream(), message2);
            } catch (MessagesException e) {
                logger.error("Failed to send message", e);
            }

        }
    }

    @Override
    public void onNewRoom(Room room) {
        room.addEventListener(this);
    }

    @Override
    public void onMessage(SymMessage symMessage) {

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

}
