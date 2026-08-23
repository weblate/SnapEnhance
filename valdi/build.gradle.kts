import org.apache.tools.ant.taskdefs.condition.Os
import javax.inject.Inject

interface InjectedExecOps { @get:Inject val execOps: ExecOperations}

plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = rootProject.ext["applicationId"].toString() + ".valdi"
    compileSdk = 37

    sourceSets {
        getByName("main") {
            assets.srcDirs("build/assets")
        }
    }
}

tasks.register("compileTypeScript") {
    description = "Compiles TypeScript files and bundles them using Rollup"
    val injected = project.objects.newInstance<InjectedExecOps>()

    doLast {
        val npxCmd = if (Os.isFamily(Os.FAMILY_WINDOWS)) "npx.cmd" else "npx"

        injected.execOps.exec {
            commandLine(npxCmd, "--yes", "tsc", "--project", "tsconfig.json")
        }

        injected.execOps.exec {
            commandLine(npxCmd, "--yes", "rollup", "--config", "rollup.config.js", "--bundleConfigAsCjs")
        }

        project.copy {
            from("build/loader.js")
            into("build/assets/valdi")
        }
    }
}

tasks.named("preBuild").configure {
    dependsOn("compileTypeScript")
}