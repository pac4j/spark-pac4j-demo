## What is this project ?

This **spark-pac4j-demo** project is a Java web application to test the [spark-pac4j library](https://github.com/pac4j/spark-pac4j) with Facebook, Twitter, form authentication, basic auth, CAS...
The **spark-pac4j** library is built to delegate authentication to a provider and be authenticated back in the protected application with a complete user profile retrieved from the provider.

## Quick start & test

To start quickly, build the project and launch the web app with jetty :

    cd spark-pac4j-demo
    mvn compile exec:java

To test, you can call a protected url by clicking on the "Protected by **xxx** : **xxx**/index.jsp" url, which will start the authentication process with the **xxx** provider.  
Or you can click on the "Authenticate with **xxx**" link, to start manually the authentication process with the **xxx** provider.
