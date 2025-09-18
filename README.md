# Odin Component Interface

Odin Component Interface (OCI) decouples components from Odin, enabling you to create custom components that can seamlessly integrate with Odin.

## Table of Contents
- [Lifecycle of a Component](#lifecycle-of-a-component)
- [Component Interface Specification](#component-interface-specification)
    - [Directory Structure](#directory-structure)
    - [Schema Definition](#schema-definition)
    - [Component.groovy Specification](#componentgroovy-specification)
    - [Special Methods](#special-methods)
    - [Accessing User Inputs](#accessing-user-inputs)
    - [Logging](#logging)
- [Contributing](#contributing)
    - [Setting Up Development Environment](#setting-up-development-environment)
    - [Setting Up IDE](#setting-up-ide)

## Lifecycle of a Component

### 1. Deploy Phase
The deploy phase is triggered when Odin receives a request to deploy a component. It consists of the following stages:

- **PreDeploy**: Executes pre-deployment actions such as creating or pulling a Docker image.
- **Deploy**: The core deployment logic of the component.
- **PostDeploy**: Executes post-deployment actions, such as cleanup tasks.
- **Discovery**: Provides discovery information (e.g., IPs or DNS) for the deployed component.
- **HealthCheck**: Verifies the health status of the deployed component.

### 2. Operate Phase
Once a component completes the deploy phase, it can expose operations to be performed on it, such as scaling or reconfiguring.

### 3. Undeploy Phase
The undeploy phase is triggered when Odin receives a request to remove a deployed component.

## Component Interface Specification

### Directory Structure
Each component follows a specific directory structure. Below is an example structure for a `mysql` component:

```bash
mysql
├── aws_rds
│   ├── operations
│   │   ├── add-reader
│   │   │   ├── schema.json
│   │   ├── remove-reader
│   │   │   ├── schema.json
│   ├── default.json
│   ├── schema.json
├── container
│   ├── operations
│   │   ├── add-reader
│   │   │   ├── schema.json
│   │   ├── remove-reader
│   │   │   ├── schema.json
│   ├── default.json
│   ├── schema.json
├── schema.json
├── default.json
├── component.groovy
```

- **Component Root Directory (`mysql`)**: The component's main directory.
- **Flavour Directories (`aws_rds`, `container`)**: Components can have multiple flavours, each with distinct configurations and operations.
- **Operations Directory**: Contains the operations that can be performed on the component.
- **Schema Files (`schema.json`)**: Defines the structure and constraints of input JSON.
    - **Root-level `schema.json`**: Defines the component definition schema. The definition schema is always independent of flavours. E.g. what version of mysql to use is independent of what flavour it is.
    - **Flavour-level `schema.json`**: Defines flavour-specific configurations.
    - **Operation-level `schema.json`**: Defines the input parameters required for specific operations.
- **Default Configuration (`default.json`)**: Specifies default values for properties. It can be placed at the root level or within flavour directories. Defaults will be applied against respective schema files of that directory.
- **Entry Script (`component.groovy`)**: Contains the main logic for component deployment and operations.

### Component.groovy Specification
The `component.groovy` file defines the component behavior. Below is an example for a `mysql` component using Terraform for provisioning:

```groovy
import com.dream11.Odin
import com.dream11.spec.HttpMethod

Odin.component {
    dslVersion "2.1.7"

    flavour {
        name "aws_rds"

        preDeploy {
            run "bash preDeploy.sh"
        }

        deploy {
            run "bash deploy.sh"

            discovery {
                run "bash discovery.sh"
            }
        }

        postDeploy {
            run "bash postDeploy.sh"
        }

        healthcheck {
            http {
                method HttpMethod.GET
                path "/healthcheck"
            }
        }

        undeploy {
            run "bash undeploy.sh"
        }

        operate {
            name "add-reader"
            run "bash addReader.sh"
        }
    }
}
```

#### Component.groovy Structure
- **`Odin.component { ... }`**: The main block where the entire component logic resides.
- **`dslVersion`**: Specifies the DSL version.
- **`flavour { ... }`**: Defines a specific flavour of the component.
    - **`name`**: Defines the flavour name.
    - **`preDeploy { ... }`**: Executes before deployment.
    - **`deploy { ... }`**: Defines the deployment logic.
        - **`discovery { ... }`**: Specifies discovery information (IP, DNS, etc.).
    - **`postDeploy { ... }`**: Executes post-deployment actions.
    - **`healthcheck { ... }`**: Defines the health-check logic.
    - **`undeploy { ... }`**: Specifies the undeployment logic.
    - **`operate { ... }`**: Defines component operations.

### Special Methods
OCI exposes special methods within `component.groovy`:

- **`run`**: Lets you execute any arbitrary OS command (e.g., `run "bash deploy.sh"`).
- **`out`**: Persists state information across invocations.
- **`getLastState()`**: Retrieves previously persisted state information.

### Accessing User Inputs
Four special objects provide access to user input:

1. **`baseConfig`**: Retrieves component-level inputs defined by the user.
2. **`flavourConfig`**: Retrieves flavour-specific inputs.
3. **`operationConfig`**: Retrieves operation-specific inputs (available only in the operate phase).
4. **`componentMetadata`**: Provides additional metadata (e.g., AWS account ID, region, etc.).

All interpolation expressions are supported using [Jinja templating](https://palletsprojects.com/projects/jinja/). Example:

```jinja
{{ baseConfig.name }}
```

### Logging
All the log messages you produce in the component must follow the log level format. If you do not provide any log level, OCI will treat them as debug logs and won't be shown to end user by default.
The log levels are as follows:
1. **`::error::`**
2. **`::warn::`**
3. **`::info::`**
4. **`::debug::`**

Any text following these log markers will be considered as the log message. Any text mentioned before the log markers will be removed. Example:
```bash
echo "::info::This is an info message"
echo "previous text ::info::This is an info message"
```

Both the above examples will log the same message: `This is an info message`.

## Contributing

### Setting Up Development Environment

#### Prerequisites
- Java 17
- Groovy 4.0.6
- IntelliJ IDEA

### Setting Up IDE
- Import the project as a Maven project.
- Predefined configurations are available for running sample components (e.g., `nginx`).

---

Feel free to contribute by submitting pull requests or opening issues in our repository!
