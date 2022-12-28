java_plugin(
    name = "lombok-java",
    generates_api = True,
    processor_class = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
    visibility = ["//visibility:public"],
    deps = ["@maven//:org_projectlombok_lombok"],
)

java_plugin(
    name = "lombok_plugin",
    data = ["lombok.config"],
    generates_api = True,
    processor_class = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor",
    deps = ["@maven//:org_projectlombok_lombok"],
)

java_library(
    name = "lombok",
    exported_plugins = [":lombok_plugin"],
    visibility = ["//visibility:public"],
    exports = ["@maven//:org_projectlombok_lombok"],
)

java_library(
    name = "maestro",
    srcs = glob([
        "src/main/java/com/maestroworkflow/*.java",
        "src/main/java/com/maestroworkflow/admin/*.java",
        "src/main/java/com/maestroworkflow/api/*.java",
        "src/main/java/com/maestroworkflow/api/annotations/*.java",
        "src/main/java/com/maestroworkflow/client/*.java",
        "src/main/java/com/maestroworkflow/models/*.java",
        "src/main/java/com/maestroworkflow/module/*.java",
        "src/main/java/com/maestroworkflow/store/*.java",
        "src/main/java/com/maestroworkflow/timers/*.java",
    ]),
    resources = glob([
        "src/main/resources/com/maestroworkflow/admin/*",
    ]),
    deps = [
        "//:lombok",
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:ch_qos_logback_logback_core",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_inject_extensions_guice_assistedinject",
        "@maven//:com_google_inject_guice",
        "@maven//:io_dropwizard_dropwizard_core",
        "@maven//:io_dropwizard_dropwizard_jersey",
        "@maven//:io_dropwizard_dropwizard_views",
        "@maven//:io_dropwizard_dropwizard_views_freemarker",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:junit_junit",
        "@maven//:org_javassist_javassist",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_slf4j_slf4j_api",
    ],
)

java_library(
    name = "maestro_test_lib",
    srcs = glob([
        "src/test/java/com/maestroworkflow/MaestroEngineTest.java",
    ]),
    deps = [
        "//:lombok",
        ":maestro",
        "@maven//:com_google_inject_guice",
        "@maven//:junit_junit",
        "@maven//:org_mockito_mockito_core",
        "@maven//:org_slf4j_slf4j_api",
    ]
)

java_test(
    name = "maestro_tests",
    size = "small",
    runtime_deps = [
        ":maestro_test_lib",
    ],
    #test_class = "junit.framework.TestCase",
)

java_binary(
    name = "demo",
    srcs = glob([
        "src/main/java/demo/*.java",
    ]),
    main_class = "demo.App",
    deps = [
        ":lombok",
        ":maestro",
        "@maven//:ch_qos_logback_logback_classic",
        "@maven//:com_fasterxml_jackson_core_jackson_core",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:com_google_code_findbugs_jsr305",
        "@maven//:com_google_guava_guava",
        "@maven//:com_google_inject_extensions_guice_assistedinject",
        "@maven//:com_google_inject_guice",
        "@maven//:io_dropwizard_dropwizard_core",
        "@maven//:io_dropwizard_dropwizard_jersey",
        "@maven//:io_dropwizard_dropwizard_views",
        "@maven//:io_dropwizard_dropwizard_views_freemarker",
        "@maven//:javax_ws_rs_javax_ws_rs_api",
        "@maven//:junit_junit",
        "@maven//:org_javassist_javassist",
        "@maven//:org_mockito_mockito_core",
    ],
)
