package utils;

import POJO.IncidentVisitor;
import POJO.MemberCheck;
import POJO.RoomMember;
import config.BotConfig;
import mongo.MongoDBClient;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.exceptions.SymException;
import org.symphonyoss.client.exceptions.UsersClientException;
import org.symphonyoss.client.model.Room;
import org.symphonyoss.symphony.pod.model.MemberInfo;
import org.symphonyoss.symphony.pod.model.MembershipList;

import java.util.ArrayList;
import java.util.List;

public class ReportGenerator {

    private SymphonyClient symphonyClient;
    private MongoDBClient mongoDBClient;

    public ReportGenerator(SymphonyClient symClient, BotConfig config) {
        symphonyClient=symClient;
        mongoDBClient = MongoDBClient.init(config.getMongoURL());
    }

    public List<RoomMember> generate(String incidentId, String streamId){
        List<MemberCheck> safeMembers = mongoDBClient.getSafeMembers(incidentId);
        List<RoomMember> roomSafetyList = new ArrayList<>();
        try {
            MembershipList membershipList = symphonyClient.getRoomMembershipClient().getRoomMembership(streamId);


            for (MemberInfo member: membershipList) {
                if (!member.getId().equals(symphonyClient.getLocalUser().getId())) {
                    RoomMember roomMember = new RoomMember();
                    roomMember.setUserId(member.getId());
                    try {
                        roomMember.setName(symphonyClient.getUsersClient().getUserFromId(member.getId()).getDisplayName());
                        roomMember.setMessage("");
                    } catch (UsersClientException e) {
                        e.printStackTrace();
                    }
                    MemberCheck check = safeMembers.stream().filter((memberCheck -> memberCheck.getUserId() == member.getId())).findAny().orElse(null);
                    if (check != null) {
                        roomMember.setSafe(check.isSafe());
                        roomMember.setMessage(check.getMessage());
                    }
                    roomSafetyList.add(roomMember);
                }
            }
            for (IncidentVisitor visitor : mongoDBClient.getVisitors(incidentId)){
                RoomMember roomMember = new RoomMember();
                roomMember.setUserId(Long.parseLong(visitor.getUserId()));
                try {
                    roomMember.setName(symphonyClient.getUsersClient().getUserFromId(Long.parseLong(visitor.getUserId())).getDisplayName());
                    roomMember.setMessage("");
                } catch (UsersClientException e) {
                    e.printStackTrace();
                }
                MemberCheck check = safeMembers.stream().filter((memberCheck -> memberCheck.getUserId() == Long.parseLong(visitor.getUserId()))).findAny().orElse(null);
                if (check != null) {
                    roomMember.setSafe(check.isSafe());
                    roomMember.setMessage(check.getMessage());
                }
                roomSafetyList.add(roomMember);
            }


        } catch (SymException e) {
            e.printStackTrace();
        }
        return roomSafetyList;
    }

    public String formatTable(List<RoomMember> report){
        String message = "<messageML><br/><br/><div>";

        message = message.concat("<table><thead><tr><td><b>Member</b></td><td><b>Accounted</b></td><td><b>Message</b></td></tr></thead><tbody>");

        for (RoomMember member: report) {
            message = message.concat("<tr><td>"+member.getName()+"</td><td>"+member.isSafe()+"</td><td>"+member.getMessage()+"</td></tr>");

        }
        message = message.concat("</tbody></table></div></messageML>");
        return message;
    }
}
