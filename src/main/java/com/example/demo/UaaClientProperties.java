package com.example.demo;

import lombok.Data;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

/**
 * Encapsulate configuration properties needed to connect and authenticate with UAA
 */
@Data
@ConfigurationProperties("uaa")
public class UaaClientProperties {
	private String clientId;
	private String clientSecret;
	private String scheme;
	private String host;
	private String port;
	private String contextPath;

	public String getAccessTokenUri() {
		return uaaBaseUriBuilder().pathSegment("oauth", "token").build().toString();
	}

	public String getUaaUri() {
		return uaaBaseUriBuilder().build().toString();

	}

	private UriBuilder uaaBaseUriBuilder() {
		final DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
		final UriBuilder uriBuilder = uriBuilderFactory.builder()
			.scheme(this.getScheme())
			.host(this.getHost())
			.port(this.getPort());

		final String contextPath = this.getContextPath();
		if (StringUtils.hasText(contextPath)) {
			return uriBuilder.pathSegment(this.getContextPath());
		}
		return uriBuilder;
	}
}
