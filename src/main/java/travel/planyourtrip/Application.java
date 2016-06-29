package travel.planyourtrip;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.webmasters.Webmasters;
import com.google.api.services.webmasters.model.ApiDataRow;
import com.google.api.services.webmasters.model.SearchAnalyticsQueryRequest;
import com.google.api.services.webmasters.model.SearchAnalyticsQueryResponse;
import com.google.api.services.webmasters.model.SitesListResponse;
import com.google.api.services.webmasters.model.WmxSite;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * TODO
 *
 * @author Marc Bellmann &lt;marc.bellmann@saxess.ag&gt;
 */
@Controller
@EnableAutoConfiguration
public class Application {

    private static final String CLIENT_ID = "959062702998-qt47dnkp24nvarl6782bgstarntihk89.apps.googleusercontent.com";
    private static final String CLIENT_SECRET = "XXX";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/loggedin";

    private static final String OAUTH_SCOPE = "https://www.googleapis.com/auth/webmasters.readonly";


    private final HttpTransport httpTransport = new NetHttpTransport();
    private final JsonFactory jsonFactory = new JacksonFactory();

    @RequestMapping("/")
    String home() throws IOException {
        return "redirect:" + flow().newAuthorizationUrl().setRedirectUri(REDIRECT_URI).build();
    }

    @RequestMapping("/loggedin")
    @ResponseBody
    List<?> loggedin(@RequestParam("code") final String code) throws IOException {
        return listSuchanfragen(code);
    }

    private List<String> listProperties(String code) throws IOException {
        final GoogleCredential credential = getGoogleCredential(code);
        final Webmasters service = getWebmasters(credential);

        final List<String> verifiedSites = new ArrayList<String>();
        final Webmasters.Sites.List request = service.sites().list();

        // Get all sites that are verified
        final SitesListResponse siteList = request.execute();
        for (final WmxSite currentSite : siteList.getSiteEntry()) {
            String permissionLevel = currentSite.getPermissionLevel();
            if (permissionLevel.equals("siteOwner")) {
                verifiedSites.add(currentSite.getSiteUrl());
            }
        }

        return verifiedSites;
    }

    List<Keyword> listSuchanfragen(final String code) throws IOException {
        final GoogleCredential credential = getGoogleCredential(code);
        final Webmasters service = getWebmasters(credential);

        final SearchAnalyticsQueryRequest request = new SearchAnalyticsQueryRequest();
        request.setDimensions(Collections.singletonList("query"));
        request.setStartDate("2016-06-01");
        request.setEndDate("2016-07-01");

        final List<Keyword> keywords = new ArrayList<>();
        final Webmasters.Searchanalytics.Query query = service.searchanalytics().query("http://planner.planyourtrip.travel/", request);

        final SearchAnalyticsQueryResponse response = query.execute();

        for (final ApiDataRow apiDataRow : response.getRows()) {
            final String keyword = apiDataRow.getKeys().get(0);

            keywords.add(new Keyword(
                    keyword,
                    apiDataRow.getClicks(),
                    apiDataRow.getImpressions(),
                    apiDataRow.getCtr(),
                    apiDataRow.getPosition()
            ));
        }

        Collections.sort(keywords);

        return keywords;
    }

    private Webmasters getWebmasters(final GoogleCredential credential) {
        // Create a new authorized API client
        return new Webmasters.Builder(httpTransport, jsonFactory, credential)
                .setApplicationName("WebmastersCommandLine")
                .build();
    }

    private GoogleCredential getGoogleCredential(final @RequestParam("code") String code) throws IOException {
        final GoogleTokenResponse response = flow().newTokenRequest(code).setRedirectUri(REDIRECT_URI).execute();
        return new GoogleCredential().setFromTokenResponse(response);
    }


    private GoogleAuthorizationCodeFlow flow() {
        return new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                jsonFactory,
                CLIENT_ID,
                CLIENT_SECRET,
                Arrays.asList(OAUTH_SCOPE)
        )
                .setAccessType("online")
                .setApprovalPrompt("auto")
                .build();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }


    private static class Keyword implements Serializable, Comparable<Keyword> {
        private final String keyword;
        private final Double klicks;
        private final Double impressionen;
        private final Double klickrate;
        private final Double position;

        private Keyword(final String keyword, final Double klicks, final Double impressionen, final Double klickrate, final Double position) {
            this.keyword = keyword;
            this.klicks = klicks;
            this.impressionen = impressionen;
            this.klickrate = klickrate;
            this.position = position;
        }

        public String getKeyword() {
            return keyword;
        }

        public Double getKlicks() {
            return klicks;
        }

        public Double getImpressionen() {
            return impressionen;
        }

        public Double getKlickrate() {
            return klickrate;
        }

        public Double getPosition() {
            return position;
        }

        @Override
        public int compareTo(final Keyword o) {
            return getPosition().compareTo(o.getPosition());
        }
    }
}
