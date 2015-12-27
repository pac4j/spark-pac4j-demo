package org.pac4j.demo.spark;

import java.util.HashMap;
import java.util.Map;

import org.pac4j.cas.client.CasClient;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;

import org.pac4j.oidc.client.OidcClient;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.sparkjava.ApplicationLogoutRoute;
import org.pac4j.sparkjava.CallbackRoute;
import org.pac4j.sparkjava.RequiresAuthenticationFilter;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.template.mustache.MustacheTemplateEngine;

import static spark.Spark.*;

@SuppressWarnings({"unchecked"})
public class SparkPac4jDemo {

	private final static Logger logger = LoggerFactory.getLogger(SparkPac4jDemo.class);

	private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

	public static void main(String[] args) {
		port(8080);
		final Config config = new DemoConfigFactory("12345678901234567890123456789012", templateEngine).build();

		get("/", (rq, rs) -> index(rq, rs, config.getClients()), templateEngine);
		final Route callback = new CallbackRoute(config);
		get("/callback", callback);
		post("/callback", callback);
        final RequiresAuthenticationFilter facebookFilter = new RequiresAuthenticationFilter(config, "FacebookClient", "", "excludedPath");
        before("/facebook", facebookFilter);
		before("/facebook/*", facebookFilter);
		before("/twitter", new RequiresAuthenticationFilter(config, "TwitterClient"));
		before("/form", new RequiresAuthenticationFilter(config, "FormClient"));
		before("/basicauth", new RequiresAuthenticationFilter(config, "IndirectBasicAuthClient"));
		before("/cas", new RequiresAuthenticationFilter(config, "CasClient"));
		before("/saml2", new RequiresAuthenticationFilter(config, "SAML2Client"));
		before("/oidc", new RequiresAuthenticationFilter(config, "OidcClient"));
		get("/facebook", SparkPac4jDemo::protectedIndex, templateEngine);
        get("/facebook/notprotected", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/twitter", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/form", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/basicauth", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/cas", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/saml2", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/saml2-metadata", (rq, rs) -> {
			SAML2Client samlclient = config.getClients().findClient(SAML2Client.class);
			samlclient.init(new SparkWebContext(rq, rs));
			return samlclient.getServiceProviderMetadataResolver().getMetadata();
		});
		get("/oidc", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/loginForm", (rq, rs) -> form(rq, config.getClients()), templateEngine);
		get("/logout", new ApplicationLogoutRoute(config));

		exception(Exception.class, (e, request, response) -> {
			response.body(templateEngine.render(new ModelAndView(new HashMap<>(), "error500.mustache")));
		});
    }

	private static ModelAndView index(final Request request, final Response response,
			final Clients clients) {
		final SparkWebContext context = new SparkWebContext(request, response);
		final Map map = new HashMap();
		map.put("profile", getUserProfile(request, response));
		map.put("facebookUrl", clients.findClient(FacebookClient.class).getRedirectionUrl(context));
		map.put("twitterUrl", clients.findClient(TwitterClient.class).getRedirectionUrl(context));
		map.put("formUrl", clients.findClient(FormClient.class).getRedirectionUrl(context));
		map.put("baUrl", clients.findClient(IndirectBasicAuthClient.class).getRedirectionUrl(context));
		map.put("casUrl", clients.findClient(CasClient.class).getRedirectionUrl(context));
		map.put("samlUrl", clients.findClient(SAML2Client.class).getRedirectionUrl(context));
		map.put("oidcUrl", clients.findClient(OidcClient.class).getRedirectionUrl(context));
		return new ModelAndView(map, "index.mustache");
	}

	private static ModelAndView form(final Request request, final Clients clients) {
		final Map map = new HashMap();
		final FormClient formClient = clients.findClient(FormClient.class);
		map.put("callbackUrl", formClient.getCallbackUrl());
		return new ModelAndView(map, "loginForm.mustache");
	}

	private static ModelAndView protectedIndex(final Request request, final Response response) {
		final Map map = new HashMap();
		map.put("profile", getUserProfile(request, response));
		return new ModelAndView(map, "protectedIndex.mustache");
	}

	private static UserProfile getUserProfile(final Request request, final Response response) {
		final SparkWebContext context = new SparkWebContext(request, response);
		final ProfileManager manager = new ProfileManager(context);
		return manager.get(true);
	}
}
