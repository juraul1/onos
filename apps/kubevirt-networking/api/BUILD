COMPILE_DEPS = CORE_DEPS + [
    "//apps/kubevirt-node/api:onos-apps-kubevirt-node-api",
    "@k8s_client//jar",
    "@k8s_model//jar",
    "@k8s_model_common//jar",
    "@k8s_model_core//jar",
    "@k8s_model_rbac//jar",
    "@k8s_model_apps//jar",
    "@k8s_model_admissionregistration//jar",
    "@k8s_model_apiextensions//jar",
    "@k8s_model_batch//jar",
    "@k8s_model_certificates//jar",
    "@k8s_model_coordination//jar",
    "@k8s_model_discovery//jar",
    "@k8s_model_flowcontrol//jar",
    "@k8s_model_metrics//jar",
    "@k8s_model_scheduling//jar",
    "@k8s_model_storageclass//jar",
    "@k8s_model_autoscaling//jar",
    "@k8s_model_events//jar",
    "@k8s_model_extensions//jar",
    "@k8s_model_networking//jar",
    "@k8s_model_node//jar",
    "@k8s_model_policy//jar",
    "@ipaddress//jar",
    "@okhttp//jar",
    "@okio//jar",
    "@logging_interceptor//jar",
    "@jackson_databind//jar",
    "@jackson_dataformat_yaml//jar",
    "@jackson_datatype_jsr310//jar",
    "@snakeyaml//jar",
    "@commons_net//jar",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
