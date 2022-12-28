<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content="">
    <title>Maestro</title>
    <link href="https://bootswatch.com/4/darkly/bootstrap.min.css" rel="stylesheet">
    <style>
        .bd-placeholder-img {
            font-size: 1.125rem;
            text-anchor: middle;
            -webkit-user-select: none;
            -moz-user-select: none;
            user-select: none;
        }
        @media (min-width: 768px) {
            .bd-placeholder-img-lg {
                font-size: 3.5rem;
            }
        }
        label {
            margin-bottom: 0;
        }
        code {
            color: white;
        }

    </style>
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-dark bg-dark">
    <div class="container-fluid">
        <a class="navbar-brand" href="#">Maestro</a>
        <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarColor01" aria-controls="navbarColor01" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        <div class="collapse navbar-collapse" id="navbarColor01">
            <ul class="navbar-nav mr-auto">
                <!--<li class="nav-item active">
                    <a class="nav-link" href="#">Dashboard
                        <span class="sr-only">(current)</span>
                    </a>
                </li>-->
            </ul>
            <form class="form-inline my-2 my-lg-0">
                <input class="form-control mr-sm-2" type="text" placeholder="Search">
                <button class="btn btn-secondary my-2 my-sm-0" type="submit">Search</button>
            </form>
        </div>
    </div>
</nav>
<main class="container-fluid">
    <div class="py-3 px-3">
        <div id="main_content"></div>
    </div>

    <!-- Workflow filters modal -->
    <div class="modal fade" id="filter_workflows" tabindex="-1" aria-labelledby="exampleModalLabel" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="exampleModalLabel">Filter workflows</h5>
                    <button type="button" class="close btn-close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">×</span>
                    </button>
                </div>
                <div class="modal-body">
                    <form id="workflow_filter">
                        <div class="mb-3">
                            <label for="exampleInputEmail1" class="form-label">Workflow ID</label>
                            <input type="email" class="form-control" id="exampleInputEmail1" aria-describedby="emailHelp">
                        </div>
                        <div class="mb-3">
                            <label for="exampleInputPassword1" class="form-label">Correlation ID</label>
                            <input type="password" class="form-control" id="exampleInputPassword1">
                        </div>
                        <div class="mb-3">
                            <label for="exampleInputPassword1" class="form-label">Workflow name</label>
                            <select name="" id="" class="form-control"></select>
                        </div>
                        <div class="mb-3">
                            <label for="exampleInputPassword1" class="form-label">Status</label>
                            <select name="" id="" class="form-control"></select>
                        </div>

                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    <button type="button" class="btn btn-primary">Apply filter</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Workflow details modal -->
    <div class="modal fade" id="workflow_data" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Workflow Details</h5>
                    <button type="button" class="close btn-close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">×</span>
                    </button>
                </div>
                <div class="modal-body">

                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Workflow input modal -->
    <div class="modal fade" id="workflow_input" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content border-light1">
                <div class="modal-header card-header">
                    <h5 class="modal-title"></h5>
                    <button type="button" class="close btn-close" data-bs-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">×</span>
                    </button>
                </div>
                <div class="modal-body">
                    <pre id="workflow_input_content"></pre>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-primary" data-bs-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

</main>

<!----------------------------------------------------------------------------------------------------------------------
Workflow list view
----------------------------------------------------------------------------------------------------------------------->
<script id="workflows_view" type="text/x-handlebars-template">
    <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3m">
        <h1 class="h2">Workflows</h1>
        <div class="btn-toolbar mb-2 mb-md-0">
            <button type="button" class="btn btn-link" data-bs-toggle="modal" data-bs-target="#filter_workflows">
                Filter workflows
            </button>
        </div>
    </div>
    <table class="table table-striped">
        <thead>
        <tr>
            <th scope="col">ID</th>
            <th scope="col">Correlation ID</th>
            <th scope="col">Workflow Name</th>
            <th scope="col">Status</th>
            <th scope="col">Creation at</th>
            <th scope="col"></th>
        </tr>
        </thead>
        <tbody>
        {{#each data}}
        <tr>
            <td scope="row"><code style="color: white">{{id}}</code></td>
            <td><code style="color: white">{{correlationId}}</code></td>
            <td>{{workflowType.clazz}}</td>
            <td>
                {{#if_eq status 'ERROR'}}
                <span class="badge badge-danger">{{status}}</span>
                {{else}}
                {{#if_eq status 'COMPLETED'}}
                <span class="badge badge-success">{{status}}</span>
                {{else}}
                <span class="badge badge-primary">{{status}}</span>
                {{/if_eq}}
                {{/if_eq}}
            </td>
            <td>{{format_date creationTime}}</td>
            <td class="text-right">
                <div class="btn-group btn-group-sm" role="group">
                    <button type="button" class="btn btn-secondary dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                        Actions
                    </button>
                    <ul class="dropdown-menu" aria-labelledby="btnGroupDrop1">
                        <li><a class="dropdown-item" href="#workflow/{{id}}" onclick="view_workflow('{{id}}');">View</a></li>
                        {{#if_eq status 'Error'}}
                        <li><a class="dropdown-item" href="#" onclick="retry_workflow('{{id}}'); return false">Retry</a></li>
                        {{/if_eq}}
                    </ul>
                </div>
            </td>
        </tr>
        {{/each}}
        </tbody>
    </table>
</script>

<!----------------------------------------------------------------------------------------------------------------------
Workflow detail view
----------------------------------------------------------------------------------------------------------------------->
<script id="workflow_detail_view" type="text/x-handlebars-template">
    <div class="container">
        <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3m">
            <h1 class="h2">Workflow Instance</h1>
            <div class="btn-toolbar mb-2 mb-md-0">
                <button type="button" class="btn btn-link" onclick="load_workflows(); return false;">
                    Back
                </button>
                <div class="btn-group btn-group-sm ml-1" role="group">
                    <button type="button" class="btn btn-success dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">
                        Actions
                    </button>
                    <ul class="dropdown-menu">
                        {{#if_eq status 'ERROR'}}
                        <li><a class="dropdown-item" href="#" onclick="retry_workflow('{{id}}'); return false">Retry workflow</a></li>
                        {{/if_eq}}
                        <li><a class="dropdown-item" href="#" onclick="cancel_workflow('{{id}}'); return false">Cancel workflow</a></li>
                    </ul>
                </div>
            </div>
        </div>

        <div class="row">
            <div class="col-md-12 col-lg-7">
                <div class="row">
                    <div class="col-md-6">
                        <label class="text-muted small">WORKFLOW ID</label>
                        <div><code style="color: white">{{id}}</code></div>
                    </div>
                    <div class="col-md-6">
                        <label class="text-muted small">CORRELATION ID</label>
                        <div><code style="color: white">{{correlationId}}</code></div>
                    </div>
                </div>
                <div class="row pt-2">
                    <div class="col-md-6">
                        <label class="text-muted small">NAME</label>
                        <div>{{workflowType.clazz}}</div>
                    </div>
                    <div class="col-md-6">
                        <label class="text-muted small">STATUS</label>
                        <div>
                            {{#if_eq status 'ERROR'}}
                            <span class="badge badge-danger">{{status}}</span>
                            {{else}}
                            {{#if_eq status 'COMPLETED'}}
                            <span class="badge badge-success">{{status}}</span>
                            {{else}}
                            <span class="badge badge-primary">{{status}}</span>
                            {{/if_eq}}
                            {{/if_eq}}
                        </div>
                    </div>
                </div>
                <div class="row pt-2">
                    <div class="col-md-6">
                        <label class="text-muted small">CREATION DATE</label>
                        <div>{{format_date creationTime}}</div>
                    </div>
                </div>

                <div class="row pt-4">
                    <div class="col-md-12">
                        <div class="card border-light">
                            <div class="card-header">
                                Initial Arguments
                            </div>
                            <div class="card-body" style="padding: 0">
                                <table class="table table-condensed" style="margin-bottom: 0;">
                                    <thead>
                                    <th style="width: 50%">Type</th>
                                    <th>Value</th>
                                    </thead>
                                    {{#each initialArgs}}
                                    <tr>
                                        <td><code>{{clazz}}</code></td>
                                        <td><code>{{value}}</code></td>
                                    </tr>
                                    {{/each}}
                                </table>
                            </div>
                        </div>
                    </div>
                </div>

                {{#if state}}
                <div class="row pt-4">
                    <div class="col-md-12">
                        <div class="card border-light">
                            <div class="card-header">
                                State
                            </div>
                            <div class="card-body" style="padding: 0">
                                <table class="table table-condensed" style="margin-bottom: 0;">
                                    <thead>
                                    <th style="width: 50%">Name</th>
                                    <th>Value</th>
                                    </thead>
                                    {{#each state}}
                                    <tr>
                                        <td><code>{{@key}}</code><span class="badge badge-light ml-2">{{clazz}}</span></td>
                                        <td><code>{{value}}</code></td>
                                    </tr>
                                    {{/each}}
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
                {{/if}}

                {{#if result}}
                <div class="row pt-4">
                    <div class="col-md-12">
                        <div class="card border-success">
                            <div class="card-header">
                                Result
                                <span class="badge badge-light ml-2">{{result.clazz}}</span>
                            </div>
                            <div class="card-body">
                                <pre style="margin-bottom: 0; white-space: pre">{{to_json_tabs result.value}}</pre>
                            </div>
                        </div>
                    </div>
                </div>
                {{/if}}

                {{#if_eq status 'ERROR'}}
                <div class="row pt-4">
                    <div class="col-md-12">
                        <div class="card border-danger bg-danger">
                            <div class="card-header">
                                Error
                            </div>
                            <div class="card-body">
                                <pre style="margin-bottom: 0;">{{statusReason}}</pre>
                            </div>
                        </div>
                    </div>
                </div>
                {{/if_eq}}
            </div>
            <div class="col-md-12 col-lg-6">

            </div>
        </div>
        <h3 class="h3 mt-5 mb-3">Operation Execution History</h3>
        <div>
            <table class="table table-striped">
                <thead>
                <tr>
                    <th scope="col">Operation</th>
                    <th scope="col">Operation input</th>
                    <th scope="col">Result</th>
                    <th scope="col">Execution date</th>
<#--                    <th scope="col"></th>-->
                </tr>
                </thead>
                <tbody>
                {{#each history}}
                <tr>
                    <td>
                        {{#if response}}
                        {{#if response.success}}
                        <span class="badge badge-success mr-1" style="min-width: 50px">Success</span>
                        {{else}}
                        <span class="badge badge-{{#if response.transient}}warning{{else}}danger{{/if}} mr-1" style="min-width: 50px">Error</span>
                        {{/if}}
                        {{else}}
                        <span class="badge badge-light mr-1" style="min-width: 50px">Pending</span>
                        {{/if}}
                        <code style="color: white">{{request.operationType.clazz}}:{{request.operationType.method}}</code><small class="text-muted mx-1">({{request.iteration}})</small>
                    </td>
                    <td>
                        {{#if response.childWorkflowInstanceId}}
                        <a href="#" onclick="view_workflow('{{response.childWorkflowInstanceId}}');">View workflow</a>
                        {{else}}
                        <a href="#" onclick="show_data('Operation input', '{{to_json request.arguments}}');return false">View input</a>
                        {{/if}}
                    </td>
                    <td>
                        {{#if response}}
                        <a href="#" onclick="show_data('Operation execution result', {{#if response.success}}'{{to_json response.result}}'{{else}}'{{to_json response.error}}'{{/if}});return false">View result</a>
                        {{/if}}
                    </td>
                    <td>{{#if response}}{{format_date response.creationTime}}{{else}}<span class="text-muted small">Not yet completed</span>{{/if}}</td>
<#--                    <td class="text-right">-->
<#--                        <div class="btn-group btn-group-sm" role="group">-->
<#--                            <button id="btnGroupDrop1" type="button" class="btn btn-secondary dropdown-toggle" data-bs-toggle="dropdown" aria-expanded="false">-->
<#--                                Actions-->
<#--                            </button>-->
<#--                            <ul class="dropdown-menu" aria-labelledby="btnGroupDrop1">-->
<#--                                <li><a class="dropdown-item" href="#" onclick="replay_workflow('{{operation_name}}', '{{iteration}}', '{{../id}}'); return false">Replay from this point</a></li>-->
<#--                            </ul>-->
<#--                        </div>-->
<#--                    </td>-->
                </tr>
                {{/each}}
                </tbody>
            </table>
        </div>
    </div>


</script>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.0.0-beta2/dist/js/bootstrap.bundle.min.js" integrity="sha384-b5kHyXgcpbZJO/tY9Ul7kGkf1S0CWuKcCD38l8YkeH8z8QjE0GmW1gYU5S9FOnJ0" crossorigin="anonymous"></script>
<script src="https://code.jquery.com/jquery-3.5.1.min.js" integrity="sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=" crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/handlebars@latest/dist/handlebars.js"></script>

<script>
    Handlebars.registerHelper('if_eq', function(a, b, opts) {
        if (a == b) {
            return opts.fn(this);
        } else {
            return opts.inverse(this);
        }
    });

    Handlebars.registerHelper("format_date", function(timestamp) {
        return new Date(timestamp * 1000).toLocaleString();

    });

    Handlebars.registerHelper('to_json', function (obj)  {
        return JSON.stringify(obj);
    });

    Handlebars.registerHelper('to_json_tabs', function (obj)  {
        return JSON.stringify(obj, null, 4);
    });

    // View definitions
    const workflows_view = Handlebars.compile($('#workflows_view').html());
    const workflow_detail_view = Handlebars.compile($('#workflow_detail_view').html());

    /**
     * Display the list of workflows view
     */
    function load_workflows() {
        document.location.hash = "#";
        $.get("/admin/workflow-instances/", function(response) {
            console.log(response);
            response = {data: response};
            $("#main_content").html(workflows_view(response));
        });
    }

    /**
     * Display the individual workflow view
     * @param id The workflow id
     */
    function view_workflow(id) {
        $.get("/admin/workflow-instances/" + id, function(response) {
            console.log(response);
            if (response.status.status === "Active") {
                response.next_inputs = response.status.data;
            }
            response.result_json = JSON.stringify(response.result, null, 4);
            $("#main_content").html(workflow_detail_view(response));
            $.get("/admin/workflow-instances/" + id + "/operation-responses", function(history_response) {
                console.log(history_response);
                response.history = history_response;
                for (let i in response.history) {
                    response.history[i].operation_input = JSON.stringify(response.history[i].operation_input);
                    console.log(response.history[i].result);
                    response.history[i].result = JSON.stringify(response.history[i].result);
                    if (response.history[i].error) {
                        response.history[i].errorMsg = JSON.stringify(response.history[i].error.value);
                    }
                }
                response.context = JSON.stringify(response.context, null, 4);
                $("#main_content").html(workflow_detail_view(response));
            });
        });
    }

    /**
     * Retry workflow action
     * @param id Workflow if
     */
    function retry_workflow(id) {
        $.post("/admin/workflow-instances/" + id +"/replay", function(response) {
            view_workflow(id);
        });
    }

    /**
     * Retry workflow action
     * @param id Workflow if
     */
    function cancel_workflow(id) {
        $.get("/admin/workflow-instances/" + id +"/cancel", function(response) {
            view_workflow(id);
        });
    }

    /**
     * Run workflows
     * @param id Workflow if
     */
    function run_workflow(id) {
        $.get("/admin/workflow-instances/" + id +"/run", function(response) {
            view_workflow(id);
        });
    }

    /**
     * Replay workflow action
     * @param operation Operation name
     * @param iteration Operation iteration
     * @param id Workflow id
     */
    function replay_workflow(operation, iteration, id) {
        $.ajax({
            url: "http://localhost:8080/workflows/" + id +"/replay",
            method: 'POST',
            data: JSON.stringify({
                operation_name: operation,
                iteration: parseInt(iteration),
            }),
            contentType: 'application/json',
            dataType: 'json',
            success: function(response) {
                view_workflow(id);
            }
        });
    }

    /**
     * Display the modal showing the input/result JSON data provided
     * @param title The title of the modal
     * @param content The object to be displayed in JSON format
     */
    function show_data(title, content) {
        try {
            content = JSON.stringify(JSON.parse(content), null, 4);
        } catch (e) {
        }

        $("#workflow_input_content").html(content);
        $("#workflow_input .modal-title").html(title);
        new bootstrap.Modal(document.getElementById("workflow_input")).toggle();
    }

    /**
     * Simple hash based routing
     */
    function route() {
        if (window.location.hash.startsWith("#workflow")) {
            let flow_id = window.location.hash.split('/')[1];
            view_workflow(flow_id);
        } else {
            load_workflows();
        }
    }

    window.addEventListener('hashchange', route);

    $(document).ready(function() {
        route();
    });

</script>
</body>
</html>