/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.eureka;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Sample Eureka service that registers with Eureka to receive and process requests.
 * This example just receives one request and exits once it receives the request after processing it.
 *
 */
public class ExampleEurekaService {

    private static ApplicationInfoManager applicationInfoManager;
    private static EurekaClient eurekaClient;

    private static synchronized ApplicationInfoManager initializeApplicationInfoManager(EurekaInstanceConfig instanceConfig) {
        if (applicationInfoManager == null) {
            InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
            applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
        }

        return applicationInfoManager;
    }

    private static synchronized EurekaClient initializeEurekaClient(ApplicationInfoManager applicationInfoManager, EurekaClientConfig clientConfig) {
        if (eurekaClient == null) {
            eurekaClient = new DiscoveryClient(applicationInfoManager, clientConfig);
        }

        return eurekaClient;
    }

    /**
     *  * This will be read by server internal discovery client. We need to salience it.
     *  
     */
    private static void injectEurekaConfiguration() throws UnknownHostException {
        String myHostName = InetAddress.getLocalHost().getHostName();
        String myServiceUrl = "http://" + myHostName + ":8080/v2/";

        System.setProperty("eureka.region", "default");// 区域必须与注册中心相同
        System.setProperty("eureka.name", "sample-servide");// 服务名修改            ？ ？

        System.setProperty("eureka.vipAddress", "provider-a-01");// 虚拟VIP地址，供Application Client查找  当前服务提供者的 applicationName
        System.setProperty("eureka.port", "8001");// 独立端口号，如果已被占用，请换之
        System.setProperty("eureka.preferSameZone", "false");// 禁用Same Zone
        System.setProperty("eureka.shouldUseDns", "false");// 禁用DNS
        System.setProperty("eureka.shouldFetchRegistry", "true");// 需要注意，必须设置为true，否则无法注册
        System.setProperty("eureka.serviceUrl.defaultZone", myServiceUrl);// 注册地址，必须与注册中心相同
        System.setProperty("eureka.serviceUrl.default.defaultZone", myServiceUrl);// 注册地址，必须与注册中心相同
    }


    public static void main(String[] args) throws UnknownHostException {

        injectEurekaConfiguration();

        DynamicPropertyFactory configInstance = com.netflix.config.DynamicPropertyFactory.getInstance();
        ApplicationInfoManager applicationInfoManager = initializeApplicationInfoManager(new MyDataCenterInstanceConfig());
        EurekaClient eurekaClient = initializeEurekaClient(applicationInfoManager, new DefaultEurekaClientConfig());

        ExampleServiceBase exampleServiceBase = new ExampleServiceBase(applicationInfoManager, eurekaClient, configInstance);
        try {
            exampleServiceBase.start();
        } finally {
            // the stop calls shutdown on eurekaClient
            exampleServiceBase.stop();
        }
    }
}
