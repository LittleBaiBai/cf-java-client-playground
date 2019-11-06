package com.example.demo;

import java.sql.Connection;
import java.util.Collections;
import java.util.Map;

import org.cloudfoundry.UnknownCloudFoundryException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.networking.NetworkingClient;
import org.cloudfoundry.networking.v1.policies.CreatePoliciesRequest;
import org.cloudfoundry.networking.v1.policies.Destination;
import org.cloudfoundry.networking.v1.policies.ListPoliciesRequest;
import org.cloudfoundry.networking.v1.policies.Policy;
import org.cloudfoundry.networking.v1.policies.Ports;
import org.cloudfoundry.networking.v1.policies.Source;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.RootProvider;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.networking.ReactorNetworkingClient;
import org.cloudfoundry.reactor.tokenprovider.ClientCredentialsGrantTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = {DemoApplication.class})
@EnableConfigurationProperties({UaaClientProperties.class})
@ActiveProfiles("secret")
class TMPNetworkConfigurerTest {

	private static final Logger log = LoggerFactory.getLogger(TMPNetworkConfigurerTest.class.getName());

	@Autowired
	private UaaClientProperties uaaProperties;

	private CloudFoundryClient cloudFoundryClient;
	private NetworkingClient networkingClient;

	private static final String DEFAULT_PROTOCOL = "tcp";
	private static final int ENVOY_PROXY_PORT = 61001;
	private static final String TARGET_APP_GUID = "2b5c5db3-17d1-4db3-a422-4ae459d9eb9c";
	private static final String DESTINATION_APP_GUID = "687fab71-c8f3-4b79-9a6e-2e247011556c";

	@BeforeEach
	void setUp() {
		ConnectionContext connectionContext = getCapiConnectionContext();
		TokenProvider tokenProvider = getTokenProvider();
		networkingClient = ReactorNetworkingClient.builder()
		                                          .connectionContext(connectionContext)
		                                          .tokenProvider(tokenProvider)
		                                          .build();
		cloudFoundryClient = ReactorCloudFoundryClient
				.builder()
				.connectionContext(connectionContext)
				.tokenProvider(tokenProvider)
				.build();
	}

	@Test
	void testNetworkGet() throws InterruptedException {
		logError("before", networkingClient.policies().list(ListPoliciesRequest.builder().build()).then()).block();
		Thread.sleep(11000);
		logError("after", networkingClient.policies().list(ListPoliciesRequest.builder().build()).then()).block();
	}

	@Test
	void testNetworkPut() throws InterruptedException {
		Source source = Source.builder().id(TARGET_APP_GUID).build();

		Destination destination = Destination.builder()
		                                     .id(DESTINATION_APP_GUID)
		                                     .protocol(DEFAULT_PROTOCOL)
		                                     .ports(Ports.builder().start(ENVOY_PROXY_PORT).end(ENVOY_PROXY_PORT).build())
		                                     .build();

		Policy policy = Policy.builder()
		                      .source(source)
		                      .destination(destination)
		                      .build();

		CreatePoliciesRequest createPoliciesRequest = CreatePoliciesRequest.builder()
		                                                                   .policy(policy)
		                                                                   .build();
		logError("before", networkingClient.policies().create(createPoliciesRequest).then()).block();
		Thread.sleep(11000);
		logError("after", networkingClient.policies().create(createPoliciesRequest).then()).block();
	}

	@Test
	void testApplicationGet() throws InterruptedException {
		cloudFoundryClient.applicationsV2().list(ListApplicationsRequest.builder().build()).block();
		Thread.sleep(11000);
		cloudFoundryClient.applicationsV2().list(ListApplicationsRequest.builder().build()).block();
	}

	@Test
	void testApplicationPut() throws InterruptedException {
		logError("before", cloudFoundryClient.applicationsV2().update(UpdateApplicationRequest.builder().applicationId(DESTINATION_APP_GUID).putAllEnvironmentJsons(Collections.emptyMap()).build()).then()).block();
		Thread.sleep(11000);
		logError("after", cloudFoundryClient.applicationsV2().update(UpdateApplicationRequest.builder().applicationId(DESTINATION_APP_GUID).putAllEnvironmentJsons(Collections.emptyMap()).build()).then()).block();
	}

	private DefaultConnectionContext getUaaConnectionContext() {
		return DefaultConnectionContext.builder()
		                               .rootProvider(new RootProvider() {
			                               @Override
			                               public Mono<String> getRoot(ConnectionContext connectionContext) {
				                               return Mono.just(uaaProperties.getUaaUri());
			                               }

			                               @Override
			                               public Mono<String> getRoot(String key, ConnectionContext connectionContext) {
				                               return Mono.just(uaaProperties.getUaaUri());
			                               }
		                               })
		                               .apiHost(uaaProperties.getHost())
		                               .secure(uaaProperties.getScheme().startsWith("https"))
		                               .port(Integer.parseInt(uaaProperties.getPort()))
		                               .build();
	}

	private DefaultConnectionContext getCapiConnectionContext() {
		return DefaultConnectionContext.builder()
		                               .apiHost(uaaProperties.getHost())
		                               .port(Integer.parseInt(uaaProperties.getPort()))
		                               .skipSslValidation(false)
		                               .secure(true)
		                               .build();
	}

	private TokenProvider getTokenProvider() {
		return ClientCredentialsGrantTokenProvider.builder()
		                                          .clientId(uaaProperties.getClientId())
		                                          .clientSecret(uaaProperties.getClientSecret())
		                                          .build();
	}

	private Mono<Void> logError(String description, Mono<Void> networkAPICall) {
		return networkAPICall
				// due to an issue in org.cloudfoundry.reactor.util.JsonCodec, decode method will return null when de-serializing
				// empty json object {} into Void.class
				.onErrorResume(NullPointerException.class, e -> Mono.empty())

				// also re-throw UnknownCloudFoundryException as ClientV2Exception to provide more details
				.onErrorResume(UnknownCloudFoundryException.class, cloudFoundryException -> Mono.error(
						new NetworkPolicyConfigurationException(cloudFoundryException, description)
				))

				.doOnSuccess(aVoid -> log.info("Succeeded ({})", description))
				.doOnError(throwable -> log.info(String.format("Failed (%s)", description), throwable));
	}

	private static class NetworkPolicyConfigurationException extends RuntimeException {
		NetworkPolicyConfigurationException(UnknownCloudFoundryException e, String message) {
			super(String.format("Failed (%s), error: %d %s", message, e.getStatusCode(), e.getPayload()), e);
		}
	}
}
