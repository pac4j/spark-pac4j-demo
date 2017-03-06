package org.pac4j.demo.spark;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.pac4j.core.client.Client;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.core.exception.HttpAction;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.profile.ProfileManager;
import org.pac4j.http.client.indirect.FormClient;

import org.pac4j.jwt.profile.JwtGenerator;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.sparkjava.ApplicationLogoutRoute;
import org.pac4j.sparkjava.CallbackRoute;
import org.pac4j.sparkjava.SecurityFilter;
import org.pac4j.sparkjava.SparkWebContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.template.mustache.MustacheTemplateEngine;

import static spark.Spark.*;

public class SparkPac4jDemo {

	private final static String JWT_SALT = "12345678901234567890123456789012";

	private final static Logger logger = LoggerFactory.getLogger(SparkPac4jDemo.class);

	private final static MustacheTemplateEngine templateEngine = new MustacheTemplateEngine();

	public static void main(String[] args) {
		port(8080);
		final Config config = new DemoConfigFactory(JWT_SALT, templateEngine).build();

		get("/", SparkPac4jDemo::index, templateEngine);
		final CallbackRoute callback = new CallbackRoute(config, null, true);
		get("/callback", callback);
		post("/callback", callback);
        final SecurityFilter facebookFilter = new SecurityFilter(config, "FacebookClient", "", "excludedPath");
        before("/facebook", facebookFilter);
		before("/facebook/*", facebookFilter);
		before("/facebookadmin", new SecurityFilter(config, "FacebookClient", "admin"));
		before("/facebookcustom", new SecurityFilter(config, "FacebookClient", "custom"));
		before("/twitter", new SecurityFilter(config, "TwitterClient,FacebookClient"));
		before("/form", new SecurityFilter(config, "FormClient"));
		before("/basicauth", new SecurityFilter(config, "IndirectBasicAuthClient"));
		before("/cas", new SecurityFilter(config, "CasClient"));
		before("/saml2", new SecurityFilter(config, "SAML2Client"));
		before("/oidc", new SecurityFilter(config, "OidcClient"));
		before("/protected", new SecurityFilter(config, null));
		before("/dba", new SecurityFilter(config, "DirectBasicAuthClient,ParameterClient"));
		before("/rest-jwt", new SecurityFilter(config, "ParameterClient"));
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
		get("/loginForm", (rq, rs) -> form(config), templateEngine);
		get("/logout", new ApplicationLogoutRoute(config, "/?defaulturlafterlogout"));
		get("/forceLogin", (rq, rs) -> forceLogin(config, rq, rs));

		/*before("/body", (request, response) -> {
			logger.debug("before /body");
		});*/
		//before("/body", new SecurityFilter(config, "AnonymousClient"));
		before("/body", new SecurityFilter(config, "HeaderClient"));
		post("/body", (request, response) -> {
			logger.debug("Body: " + request.body());
			return "done: " + getProfiles(request, response);
		});

		exception(Exception.class, (e, request, response) -> {
			logger.error("Unexpected exception", e);
			response.body(templateEngine.render(new ModelAndView(new HashMap<>(), "error500.mustache")));
		});
    }

	private static ModelAndView index(final Request request, final Response response) {
		final Map map = new HashMap();
		map.put("profiles", getProfiles(request, response));
		return new ModelAndView(map, "index.mustache");
	}

	private static ModelAndView jwt(final Request request, final Response response) {
		final SparkWebContext context = new SparkWebContext(request, response);
		final ProfileManager manager = new ProfileManager(context);
		final Optional<CommonProfile> profile = manager.get(true);
		String token = "";
		if (profile.isPresent()) {
			JwtGenerator generator = new JwtGenerator(JWT_SALT);
			token = generator.generate(profile.get());
		}
		final Map map = new HashMap();
		map.put("token", token);
		return new ModelAndView(map, "jwt.mustache");
	}

	private static ModelAndView form(final Config config) {
		final Map map = new HashMap();
		final FormClient formClient = config.getClients().findClient(FormClient.class);
		map.put("callbackUrl", formClient.getCallbackUrl());
		return new ModelAndView(map, "loginForm.mustache");
	}

	private static ModelAndView protectedIndex(final Request request, final Response response) {
		final Map map = new HashMap();
		map.put("profiles", getProfiles(request, response));
		return new ModelAndView(map, "protectedIndex.mustache");
	}

	private static List<CommonProfile> getProfiles(final Request request, final Response response) {
		final SparkWebContext context = new SparkWebContext(request, response);
		final ProfileManager manager = new ProfileManager(context);
		return manager.getAll(true);
	}

	private static ModelAndView forceLogin(final Config config, final Request request, final Response response) {
        final SparkWebContext context = new SparkWebContext(request, response);
        final String clientName = context.getRequestParameter(Clients.DEFAULT_CLIENT_NAME_PARAMETER);
		final Client client = config.getClients().findClient(clientName);
		HttpAction action;
		try {
			action = client.redirect(context);
		} catch (final HttpAction e) {
			action = e;
		}
		config.getHttpActionAdapter().adapt(action.getCode(), context);
		return null;
    }
}
