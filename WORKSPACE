load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

RULES_JVM_EXTERNAL_TAG = "4.3"

RULES_JVM_EXTERNAL_SHA = "6274687f6fc5783b589f56a2f1ed60de3ce1f99bc4e8f9edef3de43bdf7c6e74"

http_archive(
    name = "rules_jvm_external",
    sha256 = RULES_JVM_EXTERNAL_SHA,
    strip_prefix = "rules_jvm_external-%s" % RULES_JVM_EXTERNAL_TAG,
    url = "https://github.com/bazelbuild/rules_jvm_external/archive/%s.zip" % RULES_JVM_EXTERNAL_TAG,
)

load("@rules_jvm_external//:defs.bzl", "maven_install")

maven_install(
    artifacts = [
        "org.projectlombok:lombok:1.18.24",
        "com.fasterxml.jackson.core:jackson-core:2.13.4",
        "com.fasterxml.jackson.core:jackson-annotations:2.13.4",
        "com.fasterxml.jackson.core:jackson-databind:2.13.4",
        "com.google.inject:guice:5.1.0",
        "com.google.inject.extensions:guice-assistedinject:5.1.0",
        "junit:junit:4.13.2",
        "org.mockito:mockito-core:4.8.0",
        "org.javassist:javassist:3.29.2-GA",
        "ch.qos.logback:logback-classic:1.2.9",
        "io.dropwizard:dropwizard-core:2.1.1",
        "io.dropwizard:dropwizard-views:2.1.1",
        "io.dropwizard:dropwizard-jersey:2.1.1",
        "io.dropwizard:dropwizard-views-freemarker:2.1.1",
        "com.google.guava:guava:31.1-jre",
        "javax.ws.rs:javax.ws.rs-api:2.1.1",
        "com.google.code.findbugs:jsr305:3.0.2",
    ],
    repositories = [
        "https://repo1.maven.org/maven2",
    ],
)
