/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.rest.openapi;

import com.github.tomakehurst.wiremock.common.HttpsSettings;
import com.github.tomakehurst.wiremock.common.JettySettings;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.http.AdminRequestHandler;
import com.github.tomakehurst.wiremock.http.HttpServer;
import com.github.tomakehurst.wiremock.http.StubRequestHandler;
import com.github.tomakehurst.wiremock.jetty.JettyHttpServer;
import com.github.tomakehurst.wiremock.jetty.JettyHttpServerFactory;
import org.eclipse.jetty.io.NetworkTrafficListener;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Jetty 9.x {@link org.eclipse.jetty.util.ssl.SslContextFactory} removed {@code selectCipherSuites} method.
 */
public final class WireMockJettyServerFactory extends JettyHttpServerFactory {
    @Override
    public HttpServer buildHttpServer(
            final Options options, final AdminRequestHandler adminRequestHandler,
            final StubRequestHandler stubRequestHandler) {

        return new JettyHttpServer(options, adminRequestHandler, stubRequestHandler) {
            @Override
            protected ServerConnector createHttpsConnector(
                    Server server,
                    final String bindAddress, final HttpsSettings httpsSettings,
                    final JettySettings jettySettings, final NetworkTrafficListener listener) {
                final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();

                sslContextFactory.setKeyStorePath(httpsSettings.keyStorePath());
                sslContextFactory.setKeyManagerPassword(httpsSettings.keyStorePassword());
                sslContextFactory.setKeyStorePassword(httpsSettings.keyStorePassword());
                sslContextFactory.setKeyStoreType(httpsSettings.keyStoreType());
                if (httpsSettings.hasTrustStore()) {
                    sslContextFactory.setTrustStorePath(httpsSettings.trustStorePath());
                    sslContextFactory.setTrustStorePassword(httpsSettings.trustStorePassword());
                    sslContextFactory.setTrustStoreType(httpsSettings.trustStoreType());
                }
                sslContextFactory.setNeedClientAuth(httpsSettings.needClientAuth());
                sslContextFactory.setIncludeCipherSuites("TLS_DHE_RSA_WITH_AES_128_GCM_SHA256");
                sslContextFactory.setProtocol("TLSv1.3");

                final HttpConfiguration httpConfig = createHttpConfig(jettySettings);
                httpConfig.addCustomizer(new SecureRequestCustomizer());

                final int port = httpsSettings.port();

                return createServerConnector(bindAddress, jettySettings, port, listener,
                        new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(httpConfig));
            }
        };
    }
}
