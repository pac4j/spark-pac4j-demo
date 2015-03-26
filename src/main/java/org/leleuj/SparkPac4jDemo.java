package org.leleuj;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.SparkBase.setPort;

import java.util.HashMap;
import java.util.Map;

import org.pac4j.core.client.Clients;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.template.mustache.MustacheTemplateEngine;

@SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
public class SparkPac4jDemo {

	private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

	public static void main(String[] args) {
		setPort(8080);
		Clients clients = ClientsBuilder.build();
		get("/", (rq, rs) -> index(rq, rs, clients), templateEngine);
		get("/callback", new CallbackRoute(clients));
		before("/facebook", new RequiresAuthenticationFilter(clients, "FacebookClient"));
		get("/facebook", (rq, rs) -> protectedIndex(rq), templateEngine);
		get("/twitter", (rq, rs) -> protectedIndex(rq), templateEngine);
	}

	private static ModelAndView index(final Request request, final Response response,
			Clients clients) {
		SparkWebContext context = new SparkWebContext(request, response);
		CommonProfile profile = UserUtils.getProfile(request);
		Map map = new HashMap();
		map.put("profile", profile);
		map.put("facebookUrl", clients.findClient(FacebookClient.class).getRedirectionUrl(context));
		map.put("twitterUrl", clients.findClient(TwitterClient.class).getRedirectionUrl(context));
		return new ModelAndView(map, "index.mustache");
	}

	private static ModelAndView protectedIndex(final Request request) {
		CommonProfile profile = UserUtils.getProfile(request);
		Map map = new HashMap();
		map.put("profile", profile);
		return new ModelAndView(map, "protectedIndex.mustache");
	}
}
