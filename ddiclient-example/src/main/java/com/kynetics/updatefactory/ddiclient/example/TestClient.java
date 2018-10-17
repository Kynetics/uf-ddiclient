/*
 * Copyright © 2017-2018 Kynetics LLC
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 */

package com.kynetics.updatefactory.ddiclient.example;

import com.kynetics.updatefactory.ddiclient.api.ClientBuilder;
import com.kynetics.updatefactory.ddiclient.api.ServerType;
import com.kynetics.updatefactory.ddiclient.api.api.DdiRestApi;
import com.kynetics.updatefactory.ddiclient.core.UFService;
import com.kynetics.updatefactory.ddiclient.example.callback.OnTargetTokenFoundMock;
import com.kynetics.updatefactory.ddiclient.example.callback.SystemOperationMock;
import com.kynetics.updatefactory.ddiclient.example.callback.TargetDataMock;
import com.kynetics.updatefactory.ddiclient.example.callback.UserInteractionMock;
import com.kynetics.updatefactory.ddiclient.example.observer.ObserverState;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Daniele Sergio
 */
public class TestClient {
    private static final String CONFIGURATION_FILE_NAME = "update_factory.properties";
    private static final String CONFIGURATION_URL_KEY = "url";
    private static final String CONFIGURATION_TENANT_KEY = "tenant";
    private static final String CONFIGURATION_CONTROLLER_ID_KEY = "controllerId";
    private static final String CONFIGURATION_GATEWAY_TOKEN_KEY = "gatewayToken";
    public static UFService ufService;

    public static void main(String[] args) throws IOException {
        final InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME);
        final Properties properties = new Properties();
        properties.load(inputStream);

        DdiRestApi api = new ClientBuilder()
                .withBaseUrl(properties.getProperty(CONFIGURATION_URL_KEY))
                .withGatewayToken(properties.getProperty(CONFIGURATION_GATEWAY_TOKEN_KEY))
                .withHttpBuilder(new OkHttpClient.Builder())
                .withOnTargetTokenFound(new OnTargetTokenFoundMock())
                .withServerType(ServerType.UPDATE_FACTORY)
                .build();

        ufService = UFService.builder()
                .withClient(api)
                .withControllerId(properties.getProperty(CONFIGURATION_CONTROLLER_ID_KEY))
                .withTargetData(new TargetDataMock())
                .withTenant(properties.getProperty(CONFIGURATION_TENANT_KEY))
                .withUserInteraction( new UserInteractionMock())
                .withSystemOperation(new SystemOperationMock())
                .build();

        ufService.addObserver(new ObserverState());
        ufService.start();

    }

}
