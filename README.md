## What is this project ?

This **spark-pac4j-demo** project is a Java application to test the Spark java framework with Facebook, Twitter, form authentication, basic auth, CAS... everything based on pac4j.

## Quick start & test

To start quickly, build the project and launch the web app with jetty :

    cd spark-pac4j-demo
    mvn exec:java

To test, you can call a protected url by clicking on the "Protected by **xxx** : **xxx**/index.jsp" url, which will start the authentication process with the **xxx** provider.  
Or you can click on the "Authenticate with **xxx**" link, to start manually the authentication process with the **xxx** provider.
