package org.leleuj;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.SparkBase.setPort;

import java.util.HashMap;
import java.util.Map;

import org.pac4j.core.client.Clients;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.http.client.FormClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;

import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.template.mustache.MustacheTemplateEngine;

@SuppressWarnings({"unchecked", "deprecation", "rawtypes"})
public class SparkPac4jDemo {

	private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

	public static void main(String[] args) {
		setPort(8080);
		final Clients clients = ClientsBuilder.build();
		get("/", (rq, rs) -> index(rq, rs, clients), templateEngine);
		final Route callback = new CallbackRoute(clients);
		get("/callback", callback);
		post("/callback", callback);
		before("/facebook", new RequiresAuthenticationFilter(clients, "FacebookClient"));
		before("/twitter", new RequiresAuthenticationFilter(clients, "TwitterClient"));
		before("/form", new RequiresAuthenticationFilter(clients, "FormClient"));
		before("/basicauth", new RequiresAuthenticationFilter(clients, "BasicAuthClient"));
		before("/cas", new RequiresAuthenticationFilter(clients, "CasClient"));
		before("/saml", new RequiresAuthenticationFilter(clients, "Saml2Client"));
		before("/oidc", new RequiresAuthenticationFilter(clients, "OidcClient"));
		get("/facebook", (rq, rs) -> protectedIndex(rq), templateEngine);
		get("/twitter", (rq, rs) -> protectedIndex(rq), templateEngine);
		get("/form", (rq, rs) -> protectedIndex(rq), templateEngine);
		get("/basicauth", (rq, rs) -> protectedIndex(rq), templateEngine);
		get("/cas", (rq, rs) -> protectedIndex(rq), templateEngine);
		get("/saml", (rq, rs) -> protectedIndex(rq), templateEngine);
		get("/oidc", (rq, rs) -> protectedIndex(rq), templateEngine);
		get("/theForm", (rq, rs) -> form(rq, clients), templateEngine);
		get("/logout", (rq, rs) -> {
			UserUtils.logout(rq);
			rs.redirect("/");
			return null;
		});
	}

	private static ModelAndView index(final Request request, final Response response,
			final Clients clients) {
		final SparkWebContext context = new SparkWebContext(request, response);
		final CommonProfile profile = UserUtils.getProfile(request);
		final Map map = new HashMap();
		map.put("profile", profile);
		map.put("facebookUrl", clients.findClient(FacebookClient.class).getRedirectionUrl(context));
		map.put("twitterUrl", clients.findClient(TwitterClient.class).getRedirectionUrl(context));
		return new ModelAndView(map, "index.mustache");
	}

	private static ModelAndView form(final Request request, final Clients clients) {
		final Map map = new HashMap();
		final FormClient formClient = clients.findClient(FormClient.class);
		map.put("callbackUrl", formClient.getCallbackUrl());
		return new ModelAndView(map, "theForm.mustache");
	}

	private static ModelAndView protectedIndex(final Request request) {
		final CommonProfile profile = UserUtils.getProfile(request);
		final Map map = new HashMap();
		map.put("profile", profile);
		return new ModelAndView(map, "protectedIndex.mustache");
	}
}
