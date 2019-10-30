package com.example.demo;

import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.RootProvider;
import org.cloudfoundry.reactor.tokenprovider.ClientCredentialsGrantTokenProvider;
import org.cloudfoundry.reactor.uaa.ReactorUaaClient;
import reactor.core.publisher.Mono;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({UaaClientProperties.class})
class UaaConfiguration {

	@Bean
	ClientCredentialsGrantTokenProvider bindingTokenProvider(UaaClientProperties uaaProperties) {
		return ClientCredentialsGrantTokenProvider.builder()
												  .clientId(uaaProperties.getClientId())
												  .clientSecret(uaaProperties.getClientSecret())
												  .build();
	}

	@Bean
	ReactorUaaClient bindingUaaClient(ConnectionContext connectionContext,
	                                  ClientCredentialsGrantTokenProvider tokenProvider) {
		return ReactorUaaClient.builder()
							   .connectionContext(connectionContext)
							   .tokenProvider(tokenProvider)
							   .build();
	}

	@Bean
	ConnectionContext uaaConnectionContext(UaaClientProperties uaaProperties) {
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
}
