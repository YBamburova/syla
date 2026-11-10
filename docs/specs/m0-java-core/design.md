# M0 Java core design

`services/core-api` is one Maven project. `com.syla.core.CoreApplication` starts Boot. The technical `health` module has no allowed module dependencies; its controller stays in `health.internal`. It is not a new domain area. Future domain modules keep the accepted public-root/internal split.

Use Boot 3.5.16 and Modulith 1.4.14, the current patch releases on the compatible 3.5/1.4 lines. This keeps the repository's JUnit 5 rule. Boot 4/Modulith 2 are newer lines; moving to JUnit 6 is outside this slice. The exact Boot parent and Modulith BOM pin transitive versions and build plugins. Maven Enforcer pins Java to major 21 and Maven to 3.10.0. Wrapper 3.3.4 is script-only with a SHA-256 check for the Maven ZIP. The ZIP was checked against Maven Central's SHA-512 before recording SHA-256. First use needs network access.

Surefire runs `ModuleBoundariesTest`; it asserts that health was detected before calling `ApplicationModules.verify()`. Failsafe binds both integration-test and verify. `PackagedApplicationIT` starts the repackaged JAR with the same JDK, binds loopback on a random port, checks the exact health JSON, 404, and 405 over real HTTP, then stops the process in a finally block. It has a startup deadline and request timeouts. There are no database dependencies to fake or migrate yet.

A fixed output timestamp makes JAR timestamps stable. Verify this with two clean builds and compare SHA-256. Reproducibility applies to the JAR on the pinned toolchain, not Docker layer hashes or the whole hosted runner image.

The core CI job uses pinned Temurin 21.0.13+9, Maven download caching keyed by POM/wrapper properties, and `./mvnw clean verify`. It joins the explicit gate dependency set. The repository checker always expects the core POM and wrapper, even if the entire directory is deleted. Agent/web guards remain until real jobs exist.

Compose starts only the core. Its multi-stage Dockerfile builds/tests with a digest-pinned Temurin JDK and runs as UID 10001. The host port binds loopback. No PostgreSQL, RabbitMQ, or Keycloak placeholders are added. Runtime config uses Boot environment variables. This is a partial M0 setup, not the complete stack.

Sources checked 2026-11-05:

- [Boot Java/Maven support](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Modulith 1.4 compatibility](https://docs.spring.io/spring-modulith/reference/1.4/appendix.html)
- [Boot release metadata](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-starter-parent/maven-metadata.xml)
- [Modulith release metadata](https://repo.maven.apache.org/maven2/org/springframework/modulith/spring-modulith-bom/maven-metadata.xml)
- [Maven download](https://maven.apache.org/download.cgi) and [Wrapper](https://maven.apache.org/tools/wrapper/)
- [setup-java v5 action pin](https://api.github.com/repos/actions/setup-java/git/ref/tags/v5)
- [Temurin image manifest](https://hub.docker.com/v2/repositories/library/eclipse-temurin/tags/21.0.13_9-jdk-noble)
