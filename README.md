# UF-DDICLIENT #

UF-DDICLIENT is a java library that help the creation of a client to [UpdateFactory](https://www.kynetics.com/iot-platform-update-factory) or [Hawkbit](https://eclipse.org/hawkbit/) servers.

build: [![Build Status](https://travis-ci.org/Kynetics/uf-ddiclient.svg?branch=master)](https://travis-ci.org/Kynetics/uf-ddiclient)
[![Maintainability](https://api.codeclimate.com/v1/badges/e545d9c1d9256241e7f8/maintainability)](https://codeclimate.com/github/Kynetics/uf-ddiclient/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/e545d9c1d9256241e7f8/test_coverage)](https://codeclimate.com/github/Kynetics/uf-ddiclient/test_coverage)

## Install

To import this project use [jitpack](https://jitpack.io/) plugin.

## Example
Create a class that implements the Observer interface:

     private class ObserverState implements Observer {
        @Override
        public void update(Observable observable, Object o) {
            if (o instanceof UFService.SharedEvent) {
                ...
            }
        }
     }

Create the Service, add the observer and start the service:

    DdiRestApi api = new ClientBuilder()
                .withBaseUrl("https://personal.updatefactory.io")
                .withGatewayToken("[gatewayToken]")
                .withHttpBuilder(new OkHttpClient.Builder())
                .withOnTargetTokenFound(System.out::println)
                .withServerType(ServerType.UPDATE_FACTORY)
                .build();

    Map<String,String> map = new HashMap<>();
    map.put("test","tes");

    ufService = UFService.builder()
                .withClient(api)
                .withControllerId("controllerId")
                .withTargetData(()->map)
                .withTenant("test")
                .build();

    ufService.addObserver(new ObserverState());

    ufService.start();

## Third-Party Libraries
* [Retrofit](http://square.github.io/retrofit/) library

## Authors
* **Daniele Sergio** - *Initial work* - [danielesergio](https://github.com/danielesergio)

See also the list of [contributors](https://github.com/Kynetics/uf-ddiclient/graphs/contributors) who participated in this project.

## License
Copyright © 2017-2018, [Kynetics LLC](https://www.kynetics.com).
Released under the [EPLv1 License](http://www.eclipse.org/legal/epl-v10.html).