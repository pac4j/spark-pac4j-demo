package org.pac4j.demo.spark;

import java.util.HashMap;
import java.util.Map;

import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.core.profile.UserProfile;
import org.pac4j.http.client.indirect.FormClient;

import org.pac4j.jwt.profile.JwtGenerator;
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

	private final static String JWT_SALT = "12345678901234567890123456789012";

	private final static Logger logger = LoggerFactory.getLogger(SparkPac4jDemo.class);

	private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

	public static void main(String[] args) {
		port(8080);
		final Config config = new DemoConfigFactory(JWT_SALT, templateEngine).build();

		get("/", SparkPac4jDemo::index, templateEngine);
		final Route callback = new CallbackRoute(config);
		get("/callback", callback);
		post("/callback", callback);
        final RequiresAuthenticationFilter facebookFilter = new RequiresAuthenticationFilter(config, "FacebookClient", "", "excludedPath");
        before("/facebook", facebookFilter);
		before("/facebook/*", facebookFilter);
		before("/facebookadmin", new RequiresAuthenticationFilter(config, "FacebookClient", "admin"));
		before("/facebookcustom", new RequiresAuthenticationFilter(config, "FacebookClient", "custom"));
		before("/twitter", new RequiresAuthenticationFilter(config, "TwitterClient,FacebookClient"));
		before("/form", new RequiresAuthenticationFilter(config, "FormClient"));
		before("/basicauth", new RequiresAuthenticationFilter(config, "IndirectBasicAuthClient"));
		before("/cas", new RequiresAuthenticationFilter(config, "CasClient"));
		before("/saml2", new RequiresAuthenticationFilter(config, "SAML2Client"));
		before("/oidc", new RequiresAuthenticationFilter(config, "OidcClient"));
		before("/protected", new RequiresAuthenticationFilter(config, null));
		before("/dba", new RequiresAuthenticationFilter(config, "DirectBasicAuthClient,ParameterClient"));
		before("/rest-jwt", new RequiresAuthenticationFilter(config, "ParameterClient"));
		get("/facebook", SparkPac4jDemo::protectedIndex, templateEngine);
        get("/facebook/notprotected", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/facebookadmin", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/facebookcustom", SparkPac4jDemo::protectedIndex, templateEngine);
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
		get("/jwt", SparkPac4jDemo::jwt, templateEngine);
		get("/oidc", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/protected", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/dba", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/rest-jwt", SparkPac4jDemo::protectedIndex, templateEngine);
		get("/loginForm", (rq, rs) -> form(config.getClients()), templateEngine);
		get("/logout", new ApplicationLogoutRoute(config));

		exception(Exception.class, (e, request, response) -> {
			logger.error("Unexpected exception", e);
			response.body(templateEngine.render(new ModelAndView(new HashMap<>(), "error500.mustache")));
		});
    }

	private static ModelAndView index(final Request request, final Response response) {
		final Map map = new HashMap();
		map.put("profile", getUserProfile(request, response));
		return new ModelAndView(map, "index.mustache");
	}

	private static ModelAndView jwt(final Request request, final Response response) {
		final UserProfile profile = getUserProfile(request, response);
		JwtGenerator generator = new JwtGenerator(JWT_SALT);
		String token = "";
		if (profile != null) {
			token = generator.generate(profile);
		}
		final Map map = new HashMap();
		map.put("token", token);
		return new ModelAndView(map, "jwt.mustache");
	}

	private static ModelAndView form(final Clients clients) {
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
