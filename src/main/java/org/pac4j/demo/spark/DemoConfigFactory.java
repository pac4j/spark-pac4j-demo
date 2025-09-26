package org.pac4j.demo.spark;

import lombok.val;
import org.pac4j.cas.client.CasClient;
import org.pac4j.cas.config.CasConfiguration;
import org.pac4j.core.authorization.authorizer.RequireAnyRoleAuthorizer;
import org.pac4j.core.client.Clients;
import org.pac4j.core.client.direct.AnonymousClient;
import org.pac4j.core.config.Config;
import org.pac4j.core.config.ConfigFactory;
import org.pac4j.core.context.CallContext;
import org.pac4j.core.credentials.Credentials;
import org.pac4j.core.credentials.TokenCredentials;
import org.pac4j.core.credentials.authenticator.Authenticator;
import org.pac4j.core.matching.matcher.PathMatcher;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.core.util.CommonHelper;
import org.pac4j.http.client.direct.DirectBasicAuthClient;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.http.client.direct.ParameterClient;
import org.pac4j.http.client.indirect.FormClient;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;
import org.pac4j.http.credentials.authenticator.test.SimpleTestUsernamePasswordAuthenticator;
import org.pac4j.jwt.config.signature.SecretSignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.oauth.client.FacebookClient;
import org.pac4j.oauth.client.TwitterClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.OidcConfiguration;
import org.pac4j.saml.client.SAML2Client;
import org.pac4j.saml.config.SAML2Configuration;
import spark.TemplateEngine;

import java.util.Optional;

public class DemoConfigFactory implements ConfigFactory {

    private final String salt;

    private final TemplateEngine templateEngine;

    public DemoConfigFactory(final String salt, final TemplateEngine templateEngine) {
        this.salt = salt;
        this.templateEngine = templateEngine;
    }

    @Override
    public Config build(final Object... parameters) {
        val oidcConfiguration = new OidcConfiguration();
        oidcConfiguration.setClientId("343992089165-sp0l1km383i8cbm2j5nn20kbk5dk8hor.apps.googleusercontent.com");
        oidcConfiguration.setSecret("uR3D8ej1kIRPbqAFaxIE3HWh");
        oidcConfiguration.setDiscoveryURI("https://accounts.google.com/.well-known/openid-configuration");
        oidcConfiguration.setUseNonce(true);
        //oidcClient.setPreferredJwsAlgorithm(JWSAlgorithm.RS256);
        oidcConfiguration.addCustomParam("prompt", "consent");
        val oidcClient = new OidcClient(oidcConfiguration);
        oidcClient.setAuthorizationGenerator((ctx, profile) -> {
            profile.addRole("ROLE_ADMIN");
            return Optional.of(profile);
        });

        val cfg = new SAML2Configuration("resource:samlKeystore.jks", "pac4j-demo-passwd",
                                                "pac4j-demo-passwd", "resource:metadata-okta.xml");
        cfg.setMaximumAuthenticationLifetime(3600);
        cfg.setServiceProviderEntityId("http://localhost:8080/callback?client_name=SAML2Client");
        cfg.setServiceProviderMetadataPath("sp-metadata.xml");
        val saml2Client = new SAML2Client(cfg);

        val facebookClient = new FacebookClient("145278422258960", "be21409ba8f39b5dae2a7de525484da8");
        facebookClient.setCallbackUrl("https://localhost/callback");
        val twitterClient = new TwitterClient("CoxUiYwQOSFDReZYdjigBA", "2kAzunH5Btc4gRSaMr7D7MkyoJ5u1VzbOOzE8rBofs");
        // HTTP
        val formClient = new FormClient("http://localhost:8080/loginForm", new SimpleTestUsernamePasswordAuthenticator());
        val indirectBasicAuthClient = new IndirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

        // CAS
        val casConfiguration = new CasConfiguration("https://casserverpac4j.herokuapp.com/login");
        //final CasConfiguration casConfiguration = new CasConfiguration("http://localhost:8888/cas/login");
        val casClient = new CasClient(casConfiguration);

        // REST authent with JWT for a token passed in the url as the token parameter
        val parameterClient = new ParameterClient("token", new JwtAuthenticator(new SecretSignatureConfiguration(salt)));
        parameterClient.setSupportGetRequest(true);
        parameterClient.setSupportPostRequest(false);

        // basic auth
        val directBasicAuthClient = new DirectBasicAuthClient(new SimpleTestUsernamePasswordAuthenticator());

        val headerClient = new HeaderClient("Authorization", new Authenticator() {
            @Override
            public Optional<Credentials> validate(CallContext ctx, Credentials credentials) {
                final String token = ((TokenCredentials) credentials).getToken();
                if (CommonHelper.isNotBlank(token)) {
                    final CommonProfile profile = new CommonProfile();
                    profile.setId(token);
                    credentials.setUserProfile(profile);
                }
                return Optional.ofNullable(credentials);
            }
        });

        val clients = new Clients("http://localhost:8080/callback", oidcClient, saml2Client, facebookClient,
                twitterClient, formClient, indirectBasicAuthClient, casClient, parameterClient, directBasicAuthClient, new AnonymousClient(),
                headerClient);

        val config = new Config(clients);
        config.addAuthorizer("admin", new RequireAnyRoleAuthorizer("ROLE_ADMIN"));
        config.addAuthorizer("custom", new CustomAuthorizer());
        config.addMatcher("excludedPath", new PathMatcher().excludeRegex("^/facebook/notprotected$"));
        config.setHttpActionAdapter(new DemoHttpActionAdapter(templateEngine));
        return config;
    }
}
