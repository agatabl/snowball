package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.ServiceOptions;
import com.google.cloud.bigquery.storage.v1.*;
import com.google.protobuf.Descriptors;
import org.json.JSONArray;
import org.json.JSONObject;
 
import java.io.IOException;
import java.time.Instant;


import java.util.List;
import java.util.Map;
import java.util.Random;

@SpringBootApplication
@RestController
public class Application {
  String tactic  = "FRFLFRFLRFLRFLRFFLRFLRFFLRF";
  int ind = 0;
  static class Self {
    public String href;
  }
  static class WriteCommittedStream {

    final JsonStreamWriter jsonStreamWriter;

    public WriteCommittedStream(String projectId, String datasetName, String tableName) throws IOException, Descriptors.DescriptorValidationException, InterruptedException {

      try (BigQueryWriteClient client = BigQueryWriteClient.create()) {

        WriteStream stream = WriteStream.newBuilder().setType(WriteStream.Type.COMMITTED).build();
        TableName parentTable = TableName.of(projectId, datasetName, tableName);
        CreateWriteStreamRequest createWriteStreamRequest =
                CreateWriteStreamRequest.newBuilder()
                        .setParent(parentTable.toString())
                        .setWriteStream(stream)
                        .build();

        WriteStream writeStream = client.createWriteStream(createWriteStreamRequest);

        jsonStreamWriter = JsonStreamWriter.newBuilder(writeStream.getName(), writeStream.getTableSchema()).build();
      }
    }

    public ApiFuture<AppendRowsResponse> send(Arena arena) {
      Instant now = Instant.now();
      JSONArray jsonArray = new JSONArray();

      arena.state.forEach((url, playerState) -> {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("x", playerState.x);
        jsonObject.put("y", playerState.y);
        jsonObject.put("direction", playerState.direction);
        jsonObject.put("wasHit", playerState.wasHit);
        jsonObject.put("score", playerState.score);
        jsonObject.put("player", url);
        jsonObject.put("timestamp", now.getEpochSecond() * 1000 * 1000);
        jsonArray.put(jsonObject);
      });

      return jsonStreamWriter.append(jsonArray);
    }

  }

  final String projectId = ServiceOptions.getDefaultProjectId();
  final String datasetName = "snowball";
  final String tableName = "events";

  final WriteCommittedStream writeCommittedStream;

  public Application() throws Descriptors.DescriptorValidationException, IOException, InterruptedException {
    writeCommittedStream = new WriteCommittedStream(projectId, datasetName, tableName);
  }

  static class Links {
    public Self self;
  }

  static class PlayerState {
    public Integer x;
    public Integer y;
    public String direction;
    public Boolean wasHit;
    public Integer score;
  }

  static class Arena {
    public List<Integer> dims;
    public Map<String, PlayerState> state;
    
    public boolean isPlayerAhead() {
        PlayerState myplayer = state.get("https://34.107.174.9");
        for (PlayerState player: state.values()) {
            if (player != myplayer) {
                if (myplayer.direction == "N"){
                    if (player.x == myplayer.x && myplayer.y - player.y <= 3 && myplayer.y - player.y > 0) {
                        return true;
                    }

                }
                if (myplayer.direction == "S"){
                    if (player.x == myplayer.x && player.y - myplayer.y <= 3 && player.y - myplayer.y > 0) {
                        return true;
                    }
                }
                if (myplayer.direction == "E"){
                    if (player.y == myplayer.y && player.x - myplayer.x <= 3 && player.x - myplayer.x > 0) {
                        return true;
                    }
                }
                if (myplayer.direction == "W"){
                    if (player.y == myplayer.y && myplayer.x - player.x <= 3 && myplayer.x - player.x > 0) {
                        return true;
                    }

                }
            }

        }  
return false;
    }
}

  static class ArenaUpdate {
    public Links _links;
    public Arena arena;
  }

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @InitBinder
  public void initBinder(WebDataBinder binder) {
    binder.initDirectFieldAccess();
  }

  @GetMapping("/")
  public String index() {
    return "Let the battle begin!";
  }

  @PostMapping("/**")
  public String index(@RequestBody ArenaUpdate arenaUpdate) {
    System.out.println(arenaUpdate);
    // String[] commands = new String[]{"T"};
    // int i = new Random().nextInt(1);
    //return "T";
  
   writeCommittedStream.send(arenaUpdate.arena);
        ind ++;
	ind = ind%tactic.length();
    if (arenaUpdate.arena.isPlayerAhead()) {
        return "T";
    }
    return Character.toString(tactic.charAt(ind));
  }

}

