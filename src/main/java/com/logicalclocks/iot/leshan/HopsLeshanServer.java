/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *     Bosch Software Innovations - added Redis URL support with authentication
 *     Firis SA - added mDNS services registering
 *******************************************************************************/

/*
  Borrowing some code from org.eclipse.leshan.server.demo.LeshanServerDemo
 */
package com.logicalclocks.iot.leshan;

import akka.actor.ActorRef;
import com.logicalclocks.iot.leshan.listeners.HopsObservationListener;
import com.logicalclocks.iot.leshan.listeners.HopsRegistrationListener;
import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.californium.scandium.config.DtlsConnectorConfig;
import org.eclipse.californium.scandium.dtls.CertificateMessage;
import org.eclipse.californium.scandium.dtls.DTLSSession;
import org.eclipse.californium.scandium.dtls.HandshakeException;
import org.eclipse.californium.scandium.dtls.x509.CertificateVerifier;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.core.node.LwM2mResource;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeDecoder;
import org.eclipse.leshan.core.node.codec.DefaultLwM2mNodeEncoder;
import org.eclipse.leshan.core.node.codec.LwM2mNodeDecoder;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.server.californium.LeshanServerBuilder;
import org.eclipse.leshan.server.californium.impl.LeshanServer;
import org.eclipse.leshan.server.demo.servlet.ClientServlet;
import org.eclipse.leshan.server.demo.servlet.EventServlet;
import org.eclipse.leshan.server.demo.servlet.ObjectSpecServlet;
import org.eclipse.leshan.server.demo.servlet.SecurityServlet;
import org.eclipse.leshan.server.impl.FileSecurityStore;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.StaticModelProvider;
import org.eclipse.leshan.server.registration.Registration;
import org.eclipse.leshan.server.security.EditableSecurityStore;
import org.eclipse.leshan.util.SecurityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class HopsLeshanServer {
    private static final Logger logger = LoggerFactory.getLogger(HopsLeshanServer.class);

    private String coapsHost;
    private Integer coapsPort;
    private String coapHost;
    private Integer coapPort;

    private String webAddress;
    private Integer webPort;

    private LeshanServer lwServer = null;
    private ActorRef leshanActorRef;

    private final static String[] modelPaths = new String[]{"31024.xml",

            "10241.xml", "10242.xml", "10243.xml", "10244.xml", "10245.xml", "10246.xml", "10247.xml",
            "10248.xml", "10249.xml", "10250.xml",

            "2048.xml", "2049.xml", "2050.xml", "2051.xml", "2052.xml", "2053.xml", "2054.xml",
            "2055.xml", "2056.xml", "2057.xml",

            "3200.xml", "3201.xml", "3202.xml", "3203.xml", "3300.xml", "3301.xml", "3302.xml",
            "3303.xml", "3304.xml", "3305.xml", "3306.xml", "3308.xml", "3310.xml", "3311.xml",
            "3312.xml", "3313.xml", "3314.xml", "3315.xml", "3316.xml", "3317.xml", "3318.xml",
            "3319.xml", "3320.xml", "3321.xml", "3322.xml", "3323.xml", "3324.xml", "3325.xml",
            "3326.xml", "3327.xml", "3328.xml", "3329.xml", "3330.xml", "3331.xml", "3332.xml",
            "3333.xml", "3334.xml", "3335.xml", "3336.xml", "3337.xml", "3338.xml", "3339.xml",
            "3340.xml", "3341.xml", "3342.xml", "3343.xml", "3344.xml", "3345.xml", "3346.xml",
            "3347.xml", "3348.xml", "3349.xml", "3350.xml",

            "Communication_Characteristics-V1_0.xml",

            "LWM2M_Lock_and_Wipe-V1_0.xml", "LWM2M_Cellular_connectivity-v1_0.xml",
            "LWM2M_APN_connection_profile-v1_0.xml", "LWM2M_WLAN_connectivity4-v1_0.xml",
            "LWM2M_Bearer_selection-v1_0.xml", "LWM2M_Portfolio-v1_0.xml", "LWM2M_DevCapMgmt-v1_0.xml",
            "LWM2M_Software_Component-v1_0.xml", "LWM2M_Software_Management-v1_0.xml",

            "Non-Access_Stratum_NAS_configuration-V1_0.xml"};


    public HopsLeshanServer(LeshanConfig config, ActorRef leshanActorRef) {
        this.coapHost = config.coapHost();
        this.coapPort = config.coapPort();
        this.coapsHost = config.coapsHost();
        this.coapsPort = config.coapsPort();
        this.leshanActorRef = leshanActorRef;
        this.webAddress = config.webAddress();
        this.webPort = config.webPort();
    }

    public void createAndStartServer() throws Exception {
        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();
        builder.setLocalAddress(coapHost, coapPort);
        builder.setLocalSecureAddress(coapsHost, coapsPort);
        builder.setEncoder(new DefaultLwM2mNodeEncoder());
        LwM2mNodeDecoder decoder = new DefaultLwM2mNodeDecoder();
        builder.setDecoder(decoder);

        // Create CoAP Config
        NetworkConfig coapConfig;
        File configFile = new File(NetworkConfig.DEFAULT_FILE_NAME);
        if (configFile.isFile()) {
            coapConfig = new NetworkConfig();
            coapConfig.load(configFile);
        } else {
            coapConfig = LeshanServerBuilder.createDefaultNetworkConfig();
            coapConfig.store(configFile);
        }
        builder.setCoapConfig(coapConfig);

        X509Certificate serverCertificate = null;

        // Set up RPK mode
        try {
            PrivateKey privateKey = SecurityUtil.privateKey.readFromResource("credentials/server_privateKey.der");
            serverCertificate = SecurityUtil.certificate.readFromResource("credentials/server_certificate.der");
            builder.setPrivateKey(privateKey);
            builder.setCertificateChain(new X509Certificate[]{serverCertificate});

            // Use a certificate verifier which trust all certificates by default.
            DtlsConnectorConfig.Builder dtlsConfigBuilder = new DtlsConnectorConfig.Builder();
            dtlsConfigBuilder.setCertificateVerifier(new CertificateVerifier() {
                @Override
                public void verifyCertificate(CertificateMessage message, DTLSSession session)
                        throws HandshakeException {
                    // trust all means never raise HandshakeException
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            });
            builder.setDtlsConfig(dtlsConfigBuilder);

        } catch (Exception e) {
            logger.error("Unable to load embedded X.509 certificate.", e);
            System.exit(-1);
        }


        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", modelPaths));
        LwM2mModelProvider modelProvider = new StaticModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // Set securityStore & registrationStore
        EditableSecurityStore securityStore = new FileSecurityStore();
        builder.setSecurityStore(securityStore);

        // Create and start LWM2M server
        lwServer = builder.build();

        // Now prepare Jetty
        InetSocketAddress jettyAddr = new InetSocketAddress(webAddress, webPort);
        Server server = new Server(jettyAddr);
        WebAppContext root = new WebAppContext();
        root.setContextPath("/");
        root.setResourceBase(HopsLeshanServer.class.getClassLoader().getResource("webapp").toExternalForm());
        root.setParentLoaderPriority(true);
        server.setHandler(root);

        // Create Servlet
        EventServlet eventServlet = new EventServlet(lwServer, lwServer.getSecuredAddress().getPort());
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder = new ServletHolder(new SecurityServlet(securityStore, serverCertificate));
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(new ObjectSpecServlet(lwServer.getModelProvider()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");

        lwServer.getRegistrationService().addListener(new HopsRegistrationListener(leshanActorRef));
        lwServer.getObservationService().addListener(new HopsObservationListener(leshanActorRef));

        // Start Jetty & Leshan
        lwServer.start();
        server.start();
        logger.info("Web server started at {}.", server.getURI());
    }

    public Date askForCurrentTime(Registration reg) throws Exception{
        return (Date) sendRequest(reg, new ReadRequest(3,0,13))
                .orElse(Date.from(Instant.ofEpochSecond(0)));
    }

    public ObserveResponse observeRequest(Registration reg, Integer objectId) throws InterruptedException {
        return lwServer.send(reg, new ObserveRequest(objectId));
    }

    private Optional<Object> sendRequest(Registration reg, ReadRequest request) throws Exception {
        if (lwServer == null) {
            throw new Exception("First initialize LWM2M server");
        }

        try {
            ReadResponse response = lwServer.send(reg, request);
            if (response.isSuccess()) {
                return Optional.of((LwM2mResource) response.getContent())
                        .map(LwM2mResource::getValue);
            }
            else {
                logger.error("Failed reading response {} {}", response.getCode(), response.getErrorMessage());
                return Optional.empty();
            }

        } catch (InterruptedException e) {
            logger.error("Error sending request {}", e);
            return Optional.empty();
        }

    }
}
