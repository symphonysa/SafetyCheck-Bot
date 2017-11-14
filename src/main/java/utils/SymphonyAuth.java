package utils;

import config.BotConfig;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.SymphonyClientFactory;
import org.symphonyoss.client.impl.CustomHttpClient;
import org.symphonyoss.client.model.SymAuth;
import org.symphonyoss.symphony.clients.AuthenticationClient;

import javax.ws.rs.client.Client;

public class SymphonyAuth {

    public SymphonyClient init(BotConfig config) throws Exception{

        SymphonyClient symClient;

        symClient = SymphonyClientFactory.getClient(SymphonyClientFactory.TYPE.V4);


//        //Proxy config example
//        ClientConfig clientConfig = new ClientConfig();
//        clientConfig.connectorProvider(new ApacheConnectorProvider());
//        clientConfig.property(ClientProperties.PROXY_URI, "https://wwwproxy:8080");  //Or change to http..etc.
//        Client httpClient = CustomHttpClient.getClient(config.getLocalKeystorePath(),config.getLocalKeystorePassword(),config.getLocalKeystorePath(),config.getLocalKeystorePassword(),clientConfig);
//
//        AuthenticationClient authClient = new AuthenticationClient(config.getSessionAuthURL(), config.getKeyAuthUrl(),httpClient);

        AuthenticationClient authClient = new AuthenticationClient(config.getSessionAuthURL(), config.getKeyAuthUrl());


        //Set the local keystores that hold the server CA and client certificates
        authClient.setKeystores(
                config.getLocalKeystorePath(),
                config.getLocalKeystorePassword(),
                config.getBotCertPath(),
                config.getBotCertPassword());

        SymAuth symAuth = authClient.authenticate();


        //With a valid SymAuth we can now init our client.
        symClient.init(
                symAuth,
                config.getBotEmailAddress(),
                config.getAgentAPIEndpoint(),
                config.getPodAPIEndpoint()
        );

        return symClient;

    }
}
