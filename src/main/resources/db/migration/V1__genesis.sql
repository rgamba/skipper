CREATE TABLE `workflow_instances` (
    `id` varchar(100) NOT NULL,
    `correlation_id` varchar(255) NOT NULL,
    `workflow_type` varchar(255) NOT NULL,
    `result` json DEFAULT NULL,
    `state` json DEFAULT NULL,
    `initial_args` json DEFAULT NULL,
    `version` int NOT NULL DEFAULT '0',
    `status` varchar(50) NOT NULL,
    `status_reason` text,
    `callback_handler_clazz` varchar(100) DEFAULT NULL,
    `creation_time` datetime DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `correlation_id_unique` (`workflow_type`,`correlation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE `operation_requests` (
    `id` varchar(255) NOT NULL,
    `workflow_instance_id` varchar(100) NOT NULL,
    `operation_type` varchar(100) NOT NULL,
    `iteration` int NOT NULL,
    `creation_time` datetime DEFAULT NULL,
    `arguments` json DEFAULT NULL,
    `retry_strategy` json DEFAULT NULL,
    `timeout_secs` bigint DEFAULT NULL,
    `failed_attempts` int DEFAULT '0',
    PRIMARY KEY (`id`),
    KEY `workflow_instance_id` (`workflow_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE `operation_responses` (
    `id` varchar(255) NOT NULL,
    `workflow_instance_id` varchar(100) NOT NULL,
    `operation_type` varchar(100) NOT NULL,
    `iteration` int NOT NULL,
    `creation_time` datetime NOT NULL,
    `is_success` tinyint(1) NOT NULL,
    `is_transient` tinyint(1) NOT NULL,
    `operation_request_id` varchar(255) NOT NULL,
    `result` json DEFAULT NULL,
    `error` json DEFAULT NULL,
    `execution_duration_millis` bigint DEFAULT NULL,
    `child_workflow_instance_id` varchar(100) DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `workflow_instance_id` (`workflow_instance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;

CREATE TABLE `timers` (
    `id` varchar(100) NOT NULL,
    `timeout_ts_millis` bigint DEFAULT NULL,
    `handler_clazz` varchar(255) DEFAULT NULL,
    `payload` json DEFAULT NULL,
    `retries` int DEFAULT '0',
    `version` int DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `timeout` (`timeout_ts_millis`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3;