package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.applications.ListApplicationsRequest;
import org.cloudfoundry.networking.NetworkingClient;
import org.cloudfoundry.networking.v1.policies.ListPoliciesRequest;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.networking.ReactorNetworkingClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = {DemoApplication.class})
@ActiveProfiles("secret")
@Slf4j
class TMPNetworkConfigurerTest {

	@Autowired
	private ConnectionContext connectionContext;

	@Autowired
	private TokenProvider tokenProvider;

	private CloudFoundryClient cloudFoundryClient;
	private NetworkingClient networkingClient;

	private static final String DEFAULT_PROTOCOL = "tcp";
	private static final int ENVOY_PROXY_PORT = 61001;
	private static final String BOUND_APP_GUID = "21f6b282-6ba7-4d8c-841f-dc0df3a16f8c";
	private static final String BACKING_APP_GUID = "078ed4a9-afd3-4fe8-946c-0e838e0fbb4c";

	@BeforeEach
	void setUp() {
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
	void testNetwork() throws InterruptedException {
		networkingClient.policies().list(ListPoliciesRequest.builder().build()).block();
		Thread.sleep(11000);
		networkingClient.policies().list(ListPoliciesRequest.builder().build()).block();
	}

	@Test
	void testAppDetails() throws InterruptedException {
		cloudFoundryClient.applicationsV2().list(ListApplicationsRequest.builder().build()).block();
		Thread.sleep(11000);
		cloudFoundryClient.applicationsV2().list(ListApplicationsRequest.builder().build()).block();
	}
}
