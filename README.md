<p align="center">
  <img src="https://pac4j.github.io/pac4j/img/logo-spark.png" width="300" />
</p>

> This demo secures a Spark Java application with **[spark-pac4j](https://github.com/pac4j/spark-pac4j)**, the Spark Java implementation of **[pac4j](https://github.com/pac4j/pac4j)**, the security engine for Java.
> If it is useful to you, please ⭐ **[star pac4j on GitHub](https://github.com/pac4j/pac4j)**: it helps other developers discover it!

This `spark-pac4j-demo` project is a SparkJava application to test the [spark-pac4j](https://github.com/pac4j/spark-pac4j) security library with various authentication mechanisms: Facebook, Twitter, form, basic auth, CAS, SAML, OpenID Connect, JWT...

## Start & test

Build the project and launch the SparkJava app on [http://localhost:8080](http://localhost:8080):

    cd spark-pac4j-demo
    mvn clean compile exec:java

To test, you can call a protected url by clicking on the "Protected url by **xxx**" link, which will start the authentication process with the **xxx** provider.
