package com.example.demo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

/**
 * Encapsulate configuration properties needed to connect and authenticate with UAA
 */
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

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getClientId() {
		return clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public String getScheme() {
		return scheme;
	}

	public String getHost() {
		return host;
	}

	public String getPort() {
		return port;
	}

	public String getContextPath() {
		return contextPath;
	}
}
