package cucumber.json.report.submitter;
import okhttp3.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import java.io.*;
import java.util.*;

@Mojo(name = "submit-report", threadSafe = true, defaultPhase = LifecyclePhase.DEPLOY)
public class JRS extends AbstractMojo {

    private final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client = new OkHttpClient();

    @Parameter(readonly = true)
    private String jsonReports;

    @Parameter(readonly = true)
    private String serviceIP;

    @Parameter(readonly = true)
    private String servicePort;

    @Parameter(readonly = true)
    private String email;

    @Parameter(readonly = true)
    private String password;

    private String authenticateSubmission() throws IOException{

        String token = null;

        String loginDetails = "{ \"email\":\"" + email + "\", \"password\":\"" + password + "\"}";

        RequestBody requestBody = RequestBody.create(JSON, loginDetails);
        Request request = new Request.Builder()
                .url(serviceIP + ":" + servicePort)
                .post(requestBody)
                .build();

        token = client.newCall(request).execute().body().toString();

        return token;
    }

    private void submitFeature(String featureReport) throws IOException{

        RequestBody requestBody = RequestBody.create(JSON, featureReport);

        Request request = new Request.Builder()
                .url(serviceIP + ":" + servicePort)
                .header("authorization", authenticateSubmission())
                .post(requestBody)
                .build();

        client.newCall(request).execute();
    }

    private List<String> readJsonReport(String folderPath) throws Exception {
        List<String> jsonReports = new ArrayList<String>();
        File dir = new File(folderPath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                jsonReports.add(readFile(child));
            }
        }
        return jsonReports;
    }

    private String readFile (File file) throws Exception{
        BufferedReader br = new BufferedReader(new FileReader(file.getAbsoluteFile()));
        String fileContent = "";
        String stringContent;
        while ((stringContent = br.readLine()) != null){
            fileContent = fileContent + stringContent;
        }
        br.close();
        return fileContent;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            List<String> jsonContentList = readJsonReport(jsonReports);
            for (String jsonContent : jsonContentList) {
                submitFeature(jsonContent);
            }
        }
        catch (Exception e){
            System.out.println(e.toString());
        }
    }
}
