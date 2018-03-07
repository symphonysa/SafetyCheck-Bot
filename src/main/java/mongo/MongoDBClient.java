package mongo;

import POJO.Incident;
import POJO.IncidentVisitor;
import POJO.MemberCheck;
import com.mongodb.*;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class MongoDBClient {
    private static MongoDBClient instance;
    private MongoCollection<Incident> incidentCollection;
    private MongoCollection<MemberCheck> checkCollection;
    private MongoCollection<IncidentVisitor> visitorCollection;

    public static MongoDBClient init(String url){
        if(instance==null){
            instance = new MongoDBClient(url);
        }
        return instance;
    }

    protected MongoDBClient(String url) {
        MongoClientURI connectionString = new MongoClientURI(url);
        MongoClient mongoClient = new MongoClient(connectionString);
        CodecRegistry pojoCodecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build()));
        MongoDatabase database = mongoClient.getDatabase("SafetyBot");

        database = database.withCodecRegistry(pojoCodecRegistry);
        incidentCollection = database.getCollection("Incident", Incident.class);
        checkCollection = database.getCollection("MemberCheck", MemberCheck.class);
        visitorCollection = database.getCollection("IncidentVisitor", IncidentVisitor.class);

    }


    public void insertIncident(Incident incident) {
        incidentCollection.insertOne(incident);
    }

    public Incident getActiveIncidentInRoom(String streamId) {
        return incidentCollection.find(combine(eq("streamId", streamId),eq("active", true))).first();

    }

    public void insertMemberCheck(MemberCheck memberCheck) {
        if(checkCollection.find(combine(eq("incidentId",memberCheck.getIncidentId()),eq("userId", memberCheck.getUserId()))).first()==null)
            checkCollection.insertOne(memberCheck);
    }

    public List<MemberCheck> getSafeMembers(String incidentId) {
        List<MemberCheck> memberCheckList = new ArrayList<>();
        checkCollection.find(combine(eq("incidentId", incidentId),eq("safe", true))).forEach(new Block<MemberCheck>() {
            @Override
            public void apply(final MemberCheck memberCheck) {
                memberCheckList.add(memberCheck);
            }
        });
        return memberCheckList;
    }

    public Incident getReportedIncident(String emailAddress) {

        return incidentCollection.find(combine(eq("email", emailAddress), eq("active", true))).first();
    }

    public void closeIncident(Incident activeIncident) {
        incidentCollection.updateOne(eq("_id", activeIncident.getId()),set("active", false));
    }

    public void addVisitor(IncidentVisitor visitor){
        visitorCollection.insertOne(visitor);
    }

    public List<IncidentVisitor> getVisitors(String incidentId){
        List<IncidentVisitor> visitors = new ArrayList<>();
        visitorCollection.find(eq("incidentId", incidentId)).forEach(new Block<IncidentVisitor>() {
            @Override
            public void apply(final IncidentVisitor visitor) {
                visitors.add(visitor);
            }
        });
        return visitors;
    }

}
