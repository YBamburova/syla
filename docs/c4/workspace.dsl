// Target architecture; services are not implemented yet.
workspace "Syla" "Social training platform for CrossFit and functional athletes." {

    !identifiers hierarchical

    model {
        athlete = person "Athlete" "CrossFit or functional athlete who logs training and follows others."
        coach = person "Coach" "Online coach who manages athletes. Later phase." "Later"

        syla = softwareSystem "Syla" "Tracks training progress, shows recovery next to training, and connects athletes." {
            webApp = container "Web App" "Responsive UI for logging workouts, viewing progress and the social feed." "React, TypeScript, Vite, Material UI" "Web Browser"
            coreApi = container "Core API" "System of record. Modular monolith: workouts, progress, social, gyms, wearables, identity, privacy; core-managed browser sessions." "Java 21, Spring Boot, Spring Modulith"
            agentService = container "Agent Service" "AI agents: WOD parser (whiteboard photo or text) now, coach assistant later. Proposes, never commits." "Python, FastAPI"
            mcpServer = container "MCP Server" "Exposes an athlete's training data to AI clients as MCP tools. Later phase." "TypeScript, Node.js" "Later"
            database = container "Database" "Domain data, one schema per module; embeddings via pgvector. Separate provider-owned Keycloak database and credentials on the same engine." "PostgreSQL, pgvector" "Database"
            identityProvider = container "Identity Provider" "Hosts OIDC login and credential management. Owns provider identity records, not Syla domain data." "Keycloak"
            broker = container "Message Broker" "Asynchronous events between services." "RabbitMQ" "Queue"
        }

        oura = softwareSystem "Oura Cloud API" "Sleep, readiness and HRV data." "External"
        anthropic = softwareSystem "Anthropic API" "Large language models." "External"
        aiClient = softwareSystem "AI Client" "For example Claude Desktop. Later phase." "External,Later"

        athlete -> syla.webApp "Logs workouts, views progress and feed" "HTTPS"
        coach -> syla.webApp "Reviews athletes, approves AI suggestions" "HTTPS"
        syla.webApp -> syla.coreApi "Calls with opaque session cookie and CSRF protection" "HTTPS, REST/JSON"
        syla.webApp -> syla.identityProvider "Browser follows sign-in and logout redirects" "HTTPS, OIDC"
        syla.coreApi -> syla.identityProvider "OIDC code exchange, validation, logout and account lifecycle adapter" "HTTPS"
        syla.identityProvider -> syla.database "Reads/writes isolated provider database only" "JDBC"
        syla.coreApi -> syla.database "Reads and writes" "JDBC"
        syla.coreApi -> syla.agentService "Requests WOD parsing (image or text)" "HTTPS, REST/JSON"
        syla.coreApi -> syla.broker "Publishes domain events" "AMQP"
        syla.agentService -> syla.broker "Consumes events, publishes results" "AMQP"
        syla.agentService -> anthropic "Calls LLMs" "HTTPS"
        syla.coreApi -> oura "Imports recovery data" "HTTPS, OAuth2"
        aiClient -> syla.mcpServer "Asks questions about training data" "MCP"
        syla.mcpServer -> syla.coreApi "Reads athlete data" "HTTPS, REST/JSON"
    }

    views {
        systemContext syla "SystemContext" "Syla and its environment." {
            include *
            autolayout lr
        }

        container syla "Containers" "Containers inside Syla." {
            include *
            autolayout lr
        }

        styles {
            element "Person" {
                shape Person
                background #1f4e79
                color #ffffff
            }
            element "Software System" {
                background #2e75b6
                color #ffffff
            }
            element "Container" {
                background #5b9bd5
                color #ffffff
            }
            element "External" {
                background #8c8c8c
                color #ffffff
            }
            element "Database" {
                shape Cylinder
            }
            element "Queue" {
                shape Pipe
            }
            element "Web Browser" {
                shape WebBrowser
            }
            element "Later" {
                opacity 50
            }
        }
    }
}
