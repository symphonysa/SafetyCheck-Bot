import bot.ChatBot;
import config.BotConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.SymphonyClient;
import org.yaml.snakeyaml.Yaml;
import utils.SymphonyAuth;

import java.io.IOException;
import java.io.InputStream;

public class IOIMainApp {

    private BotConfig config;
    private SymphonyClient symClient;
    private final Logger LOG = LoggerFactory.getLogger(IOIMainApp.class);
    private ChatBot chatBot;

    public static void main(String [] args) {
        IOIMainApp app = new IOIMainApp();
    }

    public IOIMainApp() {
        Yaml yaml = new Yaml();
        try (InputStream in = BotConfig.class
                .getResourceAsStream("/config.yml")) {
            config = yaml.loadAs(in, BotConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            symClient = new SymphonyAuth().init(config);
            chatBot = ChatBot.getInstance(symClient, config);
        } catch (Exception e) {
            LOG.error("error", e);
        }
    }
}
