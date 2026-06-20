pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BendeyResto"

include(":app")
include(":core:designsystem")
include(":core:ui")
include(":core:navigation")
include(":core:network")
include(":core:domain")
include(":core:data")
include(":core:realtime")
include(":feature:auth")
include(":feature:dashboard")
include(":feature:pos")
include(":feature:mesas")
include(":feature:cocina")
include(":feature:caja")
include(":feature:ventas")
include(":feature:productos")
include(":feature:clientes")
include(":feature:modificadores")
include(":feature:combos")
include(":feature:configuracion")
include(":feature:repartidores")
include(":platform:printing")
include(":feature:printing")
