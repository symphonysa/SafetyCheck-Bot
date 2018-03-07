import bot.ChatBot;
import bot.RoomChatBot;
import config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.model.Room;
import org.yaml.snakeyaml.Yaml;
import utils.SymphonyAuth;

import java.io.IOException;
import java.io.InputStream;

public class BotMainApp {

    private BotConfig config;
    private SymphonyClient symClient;
    private final Logger LOG = LoggerFactory.getLogger(BotMainApp.class);
    private ChatBot chatBot;
    private RoomChatBot roomChatBot;

    public static void main(String [] args) {
        BotMainApp app = new BotMainApp();
    }

    public BotMainApp() {
        Yaml yaml = new Yaml();
        try (InputStream in = BotConfig.class
                .getResourceAsStream("/prod-config.yml")) {
            config = yaml.loadAs(in, BotConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            symClient = new SymphonyAuth().init(config);
            chatBot = ChatBot.getInstance(symClient, config);
            roomChatBot = RoomChatBot.getInstance(symClient,config);
        } catch (Exception e) {
            LOG.error("error", e);
        }
    }
}
